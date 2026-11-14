package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.MatchAnalysis.FightLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ObjectiveLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ReplayMoment;
import com.dota2coach.analysis.metric.MatchAnalysis.Timeline;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Objetivos e lutas da partida, do ponto de vista do time do jogador. */
final class MatchTimelineCalculator {

    /** Quantas lutas aparecem no relatório, escolhidas pelo tamanho da virada de ouro. */
    static final int BIGGEST_FIGHTS = 5;
    /** Trechos para rever no replay, no máximo. */
    static final int MAX_MOMENTS = 5;
    static final int MAX_FIGHT_MOMENTS = 3;
    /** Uma morte fora de luta é revista desde esse tempo antes até esse tempo depois. */
    static final int DEATH_BEFORE_SECONDS = 30;
    static final int DEATH_AFTER_SECONDS = 5;

    private static final Pattern TOWER = Pattern.compile("_tower(\\d)(?:_(top|mid|bot))?");
    private static final Pattern BARRACKS = Pattern.compile("_(melee|range)_rax_(top|mid|bot)");
    private static final Map<String, String> LANES = Map.of("top", "de cima", "mid", "do meio", "bot", "de baixo");

    private MatchTimelineCalculator() {}

    static Timeline calculate(MatchSnapshot snapshot, MatchPlayer me) {
        AnalyzedMatch match = snapshot.match();
        if (!match.replayParsed()) {
            return Timeline.EMPTY;
        }
        List<ObjectiveLine> objectives = match.objectives().stream()
                .map(o -> new ObjectiveLine(o.time(), event(o), o.side() == me.side()))
                .filter(o -> o.event() != null)
                .toList();
        List<FightLine> fights = match.teamfights().stream().map(tf -> fight(snapshot, tf, me)).toList();
        List<FightLine> biggest = fights.stream()
                .sorted(Comparator.comparingInt((FightLine f) -> Math.abs(f.goldSwing())).reversed())
                .limit(BIGGEST_FIGHTS)
                .sorted(Comparator.comparingInt(FightLine::startSeconds))
                .toList();
        return new Timeline(objectives, fights.size(),
                (int) fights.stream().filter(FightLine::youTookPart).count(),
                (int) fights.stream().filter(f -> f.goldSwing() > 0).count(),
                biggest,
                replayMoments(snapshot, me, fights));
    }

    /**
     * Até {@link #MAX_MOMENTS} trechos para rever: as lutas que mais mexeram no ouro (as suas primeiro) e, nas vagas
     * que sobram, as suas mortes que ainda não estão num desses trechos (as de fora de luta primeiro, depois as que
     * deixaram você mais tempo morto).
     */
    static List<ReplayMoment> replayMoments(MatchSnapshot snapshot, MatchPlayer me, List<FightLine> fights) {
        List<ReplayMoment> moments = new ArrayList<>();
        fights.stream()
                .sorted(Comparator.comparing((FightLine f) -> !f.youTookPart())
                        .thenComparing(f -> -Math.abs(f.goldSwing())))
                .limit(MAX_FIGHT_MOMENTS)
                .forEach(f -> moments.add(new ReplayMoment(f.startSeconds(), f.endSeconds(),
                        "luta com " + (f.goldSwing() > 0 ? "+" : "") + f.goldSwing() + " de ouro para seu time"
                                + (f.youDied() ? "; você morreu" : f.youTookPart() ? "; você participou"
                                        : "; você não estava"))));
        List<MatchPlayer.Death> deaths = me.deathLog().stream()
                .sorted(Comparator.comparing((MatchPlayer.Death d) -> fightAt(fights, d) != null)
                        .thenComparing(Comparator.comparingInt(MatchPlayer.Death::secondsDead).reversed()))
                .toList();
        for (MatchPlayer.Death d : deaths) {
            if (moments.size() >= MAX_MOMENTS) {
                break;
            }
            if (moments.stream().anyMatch(m -> d.timeSeconds() >= m.fromSeconds() && d.timeSeconds() <= m.toSeconds())) {
                continue;
            }
            String killer = d.killerHeroId() == null ? "" : " para " + snapshot.heroName(d.killerHeroId());
            FightLine f = fightAt(fights, d);
            moments.add(f == null
                    ? new ReplayMoment(Math.max(0, d.timeSeconds() - DEATH_BEFORE_SECONDS),
                            d.timeSeconds() + DEATH_AFTER_SECONDS, "você morreu fora de luta" + killer)
                    : new ReplayMoment(f.startSeconds(), f.endSeconds(), "você morreu" + killer + " numa luta com "
                            + (f.goldSwing() > 0 ? "+" : "") + f.goldSwing() + " de ouro para seu time"));
        }
        moments.sort(Comparator.comparingInt(ReplayMoment::fromSeconds));
        return moments;
    }

    private static FightLine fightAt(List<FightLine> fights, MatchPlayer.Death d) {
        return fights.stream()
                .filter(f -> d.timeSeconds() >= f.startSeconds() && d.timeSeconds() <= f.endSeconds())
                .findFirst().orElse(null);
    }

    private static FightLine fight(MatchSnapshot snapshot, AnalyzedMatch.Teamfight tf, MatchPlayer me) {
        int swing = 0;
        int buybacks = 0;
        List<String> yourTeamDead = new ArrayList<>();
        List<String> enemiesDead = new ArrayList<>();
        boolean tookPart = false;
        boolean died = false;
        for (AnalyzedMatch.FightPlayer p : tf.players()) {
            boolean yours = p.side() == me.side();
            swing += yours ? p.goldDelta() : -p.goldDelta();
            for (int i = 0; i < p.deaths(); i++) {
                (yours ? yourTeamDead : enemiesDead).add(snapshot.heroName(p.heroId()));
            }
            if (yours) {
                buybacks += p.buybacks();
            }
            if (p.heroId() == me.heroId() && yours) {
                tookPart = p.damage() > 0 || p.healing() > 0 || p.deaths() > 0;
                died = p.deaths() > 0;
            }
        }
        return new FightLine(tf.start(), tf.end(), swing, yourTeamDead, enemiesDead, tookPart, died, buybacks);
    }

    /** Texto do objetivo para vir depois de quem conquistou, ou nulo se a construção não é reconhecida. */
    static String event(AnalyzedMatch.Objective o) {
        return switch (o.kind()) {
            case ROSHAN -> "matou o Roshan";
            case FIRST_BLOOD -> "tirou o first blood";
            case BUILDING -> building(o.building());
        };
    }

    private static String building(String key) {
        if (key == null) {
            return null;
        }
        if (key.endsWith("_fort")) {
            return "destruiu o Ancient";
        }
        Matcher tower = TOWER.matcher(key);
        if (tower.find()) {
            String lane = tower.group(2) == null ? "" : " " + LANES.get(tower.group(2));
            return "derrubou a torre T" + tower.group(1) + lane;
        }
        Matcher rax = BARRACKS.matcher(key);
        if (rax.find()) {
            String kind = rax.group(1).equals("melee") ? "corpo a corpo" : "à distância";
            return "derrubou o barracks " + kind + " " + LANES.get(rax.group(2));
        }
        return null;
    }
}
