package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.MatchAnalysis.DeathLine;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathPhaseLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ItemLine;
import com.dota2coach.analysis.metric.MatchAnalysis.LaneOpponent;
import com.dota2coach.analysis.metric.MatchAnalysis.Laning;
import com.dota2coach.analysis.metric.MatchAnalysis.Line;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamGold;
import com.dota2coach.analysis.metric.MatchAnalysis.Usual;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Calcula a análise de uma partida escolhida pelo jogador. */
public class MatchAnalysisCalculator {

    static final int LANING_MINUTE = 10;
    /** A vantagem de ouro do time aparece a cada tantos minutos, até o fim da partida. */
    static final int GOLD_EVERY_MINUTES = 10;
    /** Rotas físicas da OpenDota que são rotas de verdade (1 embaixo, 2 meio, 3 em cima); 4 e 5 são selva. */
    private static final int LAST_REAL_LANE = 3;

    /**
     * @param accountId conta do jogador, que precisa estar na partida com os dados públicos
     * @param position posição que o jogador informou, ou nulo
     * @param latestXRay último Raio-X salvo do jogador, para comparar com o padrão dele; pode ser nulo
     * @throws IllegalArgumentException se a conta não aparece na partida
     */
    public MatchAnalysis analyze(MatchSnapshot snapshot, long accountId, Position position, ProfileXRay latestXRay) {
        AnalyzedMatch match = snapshot.match();
        MatchPlayer me = match.player(accountId).orElseThrow(() -> new IllegalArgumentException(
                "A conta " + accountId + " não aparece na partida " + match.matchId()
                        + " (ou está com os dados públicos desligados)."));
        String heroName = snapshot.heroName(me.heroId());
        Map<String, Item> itemsByKey = snapshot.items().values().stream()
                .collect(Collectors.toMap(Item::key, Function.identity(), (a, b) -> a));
        return new MatchAnalysis(
                match.matchId(),
                match.startTime(),
                match.duration(),
                (me.side() == Side.RADIANT) == match.radiantWin(),
                match.gameMode(),
                match.ranked(),
                match.replayParsed(),
                heroName,
                position,
                me.lane(),
                new Line(me.kills(), me.deaths(), me.assists(), me.lastHits(), me.denies(), me.goldPerMin(),
                        me.xpPerMin(), me.heroDamage(), me.towerDamage(), me.heroHealing(), me.netWorth(),
                        me.level()),
                killParticipation(match, me),
                me.benchmarkPercentiles(),
                me.lastHitsAt10Percentile(),
                laning(snapshot, me),
                deaths(snapshot, me),
                deathsByPhase(me, latestXRay),
                items(me, itemsByKey, heroName, latestXRay),
                me.observersPlaced(),
                me.sentriesPlaced(),
                teamGold(match, me),
                usual(latestXRay, heroName),
                MatchContextCalculator.calculate(snapshot, me),
                MatchTimelineCalculator.calculate(snapshot, me),
                MatchItemsCalculator.calculate(snapshot, me, itemsByKey));
    }

    static Double killParticipation(AnalyzedMatch match, MatchPlayer me) {
        int teamKills = match.players().stream().filter(p -> p.side() == me.side()).mapToInt(MatchPlayer::kills)
                .sum();
        return teamKills == 0 ? null : Math.min(1.0, (me.kills() + me.assists()) / (double) teamKills);
    }

    static Laning laning(MatchSnapshot snapshot, MatchPlayer me) {
        if (me.lastHitsByMinute().isEmpty()) {
            return Laning.EMPTY;
        }
        List<LaneOpponent> opponents = me.physicalLane() == null || me.physicalLane() > LAST_REAL_LANE
                ? List.of()
                : snapshot.match().players().stream()
                        .filter(p -> p.side() != me.side() && Objects.equals(p.physicalLane(), me.physicalLane()))
                        .map(p -> new LaneOpponent(snapshot.heroName(p.heroId()),
                                MatchPlayer.atMinute(p.lastHitsByMinute(), LANING_MINUTE),
                                MatchPlayer.atMinute(p.deniesByMinute(), LANING_MINUTE),
                                MatchPlayer.atMinute(p.goldByMinute(), LANING_MINUTE),
                                MatchPlayer.atMinute(p.xpByMinute(), LANING_MINUTE)))
                        .toList();
        return new Laning(
                MatchPlayer.atMinute(me.lastHitsByMinute(), LANING_MINUTE),
                MatchPlayer.atMinute(me.deniesByMinute(), LANING_MINUTE),
                MatchPlayer.atMinute(me.goldByMinute(), LANING_MINUTE),
                MatchPlayer.atMinute(me.xpByMinute(), LANING_MINUTE),
                me.laneEfficiencyPct(),
                opponents);
    }

    static List<DeathLine> deaths(MatchSnapshot snapshot, MatchPlayer me) {
        return me.deathLog().stream()
                .map(d -> new DeathLine(d.timeSeconds(),
                        MatchDetailsCalculator.phaseLabel(MatchDetailsCalculator.phaseIndex(d.timeSeconds())),
                        d.killerHeroId() == null ? null : snapshot.heroName(d.killerHeroId()),
                        d.secondsDead()))
                .toList();
    }

    static List<DeathPhaseLine> deathsByPhase(MatchPlayer me, ProfileXRay latestXRay) {
        if (me.deathLog().isEmpty() && me.lastHitsByMinute().isEmpty()) {
            return List.of();
        }
        int phases = MatchDetailsCalculator.PHASE_LIMITS_MIN.length + 1;
        int[] counts = new int[phases];
        me.deathLog().forEach(d -> counts[MatchDetailsCalculator.phaseIndex(d.timeSeconds())]++);
        Map<String, Double> usual = latestXRay == null || latestXRay.details() == null ? Map.of()
                : latestXRay.details().deathsByPhase().stream()
                        .collect(Collectors.toMap(ProfileXRay.DeathPhase::label,
                                ProfileXRay.DeathPhase::deathsPerMatch, (a, b) -> a));
        List<DeathPhaseLine> lines = new ArrayList<>();
        for (int i = 0; i < phases; i++) {
            String label = MatchDetailsCalculator.phaseLabel(i);
            lines.add(new DeathPhaseLine(label, counts[i], usual.get(label)));
        }
        return lines;
    }

    static List<ItemLine> items(MatchPlayer me, Map<String, Item> itemsByKey, String heroName,
            ProfileXRay latestXRay) {
        Map<String, Double> usual = latestXRay == null || latestXRay.details() == null ? Map.of()
                : latestXRay.details().itemTimings().stream()
                        .filter(t -> t.heroName().equals(heroName))
                        .collect(Collectors.toMap(ProfileXRay.ItemTiming::itemName,
                                ProfileXRay.ItemTiming::medianMinute, (a, b) -> a));
        return MatchItemsCalculator.firstCoreItems(me, itemsByKey).entrySet().stream()
                .map(e -> {
                    String name = itemsByKey.get(e.getKey()).name();
                    return new ItemLine(name, e.getValue(), usual.get(name));
                })
                .toList();
    }

    static List<TeamGold> teamGold(AnalyzedMatch match, MatchPlayer me) {
        int sign = me.side() == Side.RADIANT ? 1 : -1;
        List<TeamGold> lines = new ArrayList<>();
        for (int minute = GOLD_EVERY_MINUTES; minute < match.radiantGoldAdvantage().size();
                minute += GOLD_EVERY_MINUTES) {
            Integer radiant = MatchPlayer.atMinute(match.radiantGoldAdvantage(), minute);
            if (radiant != null) {
                lines.add(new TeamGold(minute, sign * radiant));
            }
        }
        return lines;
    }

    static Usual usual(ProfileXRay x, String heroName) {
        if (x == null) {
            return null;
        }
        ProfileXRay.HeroLine hero = x.heroes().stream().filter(h -> h.name().equals(heroName)).findFirst()
                .orElse(null);
        ProfileXRay.MatchDetails details = x.details() == null ? ProfileXRay.MatchDetails.EMPTY : x.details();
        Double heroLastHits = details.laningByHero().stream().filter(l -> l.heroName().equals(heroName))
                .map(ProfileXRay.HeroLaning::lastHitsAt10).findFirst().orElse(null);
        return new Usual(
                x.collectedAt(),
                hero == null ? 0 : hero.recent().games(),
                hero == null ? null : hero.goldPerMin(),
                hero == null ? null : hero.xpPerMin(),
                hero == null ? null : hero.kda(),
                heroLastHits != null ? heroLastHits : details.lastHitsAt10(),
                heroLastHits != null,
                details.deniesAt10(),
                details.secondsDeadPerMatch());
    }
}
