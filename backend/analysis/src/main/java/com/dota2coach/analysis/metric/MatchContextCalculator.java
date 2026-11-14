package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.MatchAnalysis.Context;
import com.dota2coach.analysis.metric.MatchAnalysis.DamageShare;
import com.dota2coach.analysis.metric.MatchAnalysis.Matchup;
import com.dota2coach.analysis.metric.MatchAnalysis.PhasePower;
import com.dota2coach.analysis.metric.MatchAnalysis.Pick;
import com.dota2coach.analysis.metric.MatchAnalysis.RoleCount;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamProfile;
import com.dota2coach.core.enums.DamageType;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.Hero;
import com.dota2coach.core.model.HeroDurations;
import com.dota2coach.core.model.HeroMatchups;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Contexto de uma partida: draft, composição, perfil de dano, matchups e força dos times por duração. */
final class MatchContextCalculator {

    /** Faixas de duração, em minutos: até 20, 20-30, 30-40, 40-50 e 50 ou mais. */
    static final int[] POWER_LIMITS_MIN = {20, 30, 40, 50};
    /** Partidas públicas mínimas de um herói numa faixa de duração para entrar na média do time. */
    static final int MIN_DURATION_GAMES = 50;
    /** Partidas públicas mínimas do matchup para mostrar. */
    static final int MIN_MATCHUP_GAMES = 20;

    /** Funções oficiais da OpenDota, em português, na ordem em que aparecem no relatório. */
    static final Map<String, String> ROLES = new LinkedHashMap<>();

    static {
        ROLES.put("Carry", "Carry");
        ROLES.put("Nuker", "Dano mágico em rajada (nuker)");
        ROLES.put("Disabler", "Controle (disabler)");
        ROLES.put("Initiator", "Iniciador");
        ROLES.put("Pusher", "Derruba torres (pusher)");
        ROLES.put("Durable", "Resistente");
        ROLES.put("Escape", "Mobilidade (escape)");
        ROLES.put("Support", "Suporte");
    }

    private MatchContextCalculator() {}

    static Context calculate(MatchSnapshot snapshot, MatchPlayer me) {
        AnalyzedMatch match = snapshot.match();
        Side mySide = me.side();
        boolean orderKnown = !match.picks().isEmpty() && match.picks().stream().allMatch(p -> p.order() != null);
        List<Pick> picks = match.picks().stream()
                .sorted(Comparator.comparing(AnalyzedMatch.DraftPick::order,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> new Pick(p.order(), snapshot.heroName(p.heroId()), p.side() == mySide,
                        p.heroId() == me.heroId() && p.side() == mySide))
                .toList();
        Integer yourTeamPickNumber = null;
        if (orderKnown) {
            int n = 0;
            for (Pick p : picks) {
                if (p.yourTeam()) {
                    n++;
                    if (p.you()) {
                        yourTeamPickNumber = n;
                    }
                }
            }
        }
        List<MatchPlayer> mine = match.players().stream().filter(p -> p.side() == mySide).toList();
        List<MatchPlayer> theirs = match.players().stream().filter(p -> p.side() != mySide).toList();
        boolean won = (mySide == Side.RADIANT) == match.radiantWin();
        return new Context(
                picks,
                orderKnown,
                yourTeamPickNumber,
                List.of(team(snapshot, mine, true, won), team(snapshot, theirs, false, !won)),
                damageShare(List.of(me.damageReceivedBySource()), snapshot.damageTypes()),
                matchups(snapshot, me, theirs),
                power(snapshot, mine, theirs));
    }

    private static TeamProfile team(MatchSnapshot snapshot, List<MatchPlayer> players, boolean yours, boolean won) {
        Map<String, Integer> roles = new LinkedHashMap<>();
        ROLES.keySet().forEach(r -> roles.put(r, 0));
        for (MatchPlayer p : players) {
            Hero hero = snapshot.heroes().get(p.heroId());
            if (hero != null) {
                hero.roles().stream().filter(roles::containsKey).forEach(r -> roles.merge(r, 1, Integer::sum));
            }
        }
        return new TeamProfile(
                yours,
                won,
                players.stream().map(p -> snapshot.heroName(p.heroId())).toList(),
                damageShare(players.stream().map(MatchPlayer::damageDealtBySource).toList(), snapshot.damageTypes()),
                roles.entrySet().stream().filter(e -> e.getValue() > 0)
                        .map(e -> new RoleCount(ROLES.get(e.getKey()), e.getValue())).toList());
    }

    static DamageShare damageShare(List<Map<String, Long>> bySource, Map<String, DamageType> types) {
        Map<DamageType, Long> totals = new EnumMap<>(DamageType.class);
        for (Map<String, Long> sources : bySource) {
            sources.forEach((source, value) -> {
                DamageType type = MatchPlayer.BASIC_ATTACK.equals(source) ? DamageType.PHYSICAL
                        : types.getOrDefault(source, DamageType.OTHER);
                totals.merge(type, value, Long::sum);
            });
        }
        long total = totals.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return DamageShare.NONE;
        }
        return new DamageShare(total,
                totals.getOrDefault(DamageType.PHYSICAL, 0L) / (double) total,
                totals.getOrDefault(DamageType.MAGICAL, 0L) / (double) total,
                totals.getOrDefault(DamageType.PURE, 0L) / (double) total,
                totals.getOrDefault(DamageType.OTHER, 0L) / (double) total);
    }

    private static List<Matchup> matchups(MatchSnapshot snapshot, MatchPlayer me, List<MatchPlayer> enemies) {
        HeroMatchups matchups = snapshot.playerMatchups();
        if (matchups == null || matchups.heroId() != me.heroId()) {
            return List.of();
        }
        boolean realLane = me.physicalLane() != null && me.physicalLane() <= 3;
        List<Matchup> lines = new ArrayList<>();
        for (MatchPlayer enemy : enemies) {
            HeroMatchups.Record record = matchups.againstHero().get(enemy.heroId());
            if (record == null || record.games() < MIN_MATCHUP_GAMES) {
                continue;
            }
            lines.add(new Matchup(snapshot.heroName(enemy.heroId()),
                    realLane && Objects.equals(enemy.physicalLane(), me.physicalLane()),
                    record.games(), record.winRate()));
        }
        lines.sort(Comparator.comparing(Matchup::laneOpponent).reversed()
                .thenComparing(Comparator.comparingDouble(Matchup::winRate)));
        return lines;
    }

    private static List<PhasePower> power(MatchSnapshot snapshot, List<MatchPlayer> mine, List<MatchPlayer> theirs) {
        List<PhasePower> lines = new ArrayList<>();
        for (int i = 0; i <= POWER_LIMITS_MIN.length; i++) {
            int from = i == 0 ? 0 : POWER_LIMITS_MIN[i - 1] * 60;
            int to = i == POWER_LIMITS_MIN.length ? Integer.MAX_VALUE : POWER_LIMITS_MIN[i] * 60;
            Double yours = teamWinRate(snapshot, mine, from, to);
            Double enemy = teamWinRate(snapshot, theirs, from, to);
            if (yours != null || enemy != null) {
                lines.add(new PhasePower(phaseLabel(i), yours, enemy));
            }
        }
        return lines;
    }

    private static Double teamWinRate(MatchSnapshot snapshot, List<MatchPlayer> team, int from, int to) {
        var rates = team.stream()
                .map(p -> snapshot.heroDurations().get(p.heroId()))
                .filter(Objects::nonNull)
                .map(d -> heroWinRate(d, from, to))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return rates.isPresent() ? rates.getAsDouble() : null;
    }

    static Double heroWinRate(HeroDurations durations, int fromSeconds, int toSeconds) {
        int games = 0;
        int wins = 0;
        for (HeroDurations.Bin bin : durations.bins()) {
            if (bin.fromSeconds() >= fromSeconds && bin.fromSeconds() < toSeconds) {
                games += bin.games();
                wins += bin.wins();
            }
        }
        return games < MIN_DURATION_GAMES ? null : wins / (double) games;
    }

    static String phaseLabel(int index) {
        if (index == 0) {
            return "até " + POWER_LIMITS_MIN[0] + " min";
        }
        if (index == POWER_LIMITS_MIN.length) {
            return POWER_LIMITS_MIN[index - 1] + " min ou mais";
        }
        return POWER_LIMITS_MIN[index - 1] + " a " + POWER_LIMITS_MIN[index] + " min";
    }
}
