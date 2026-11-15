package com.dota2coach.analysis.evolution;

import com.dota2coach.analysis.evolution.FocusGoalCheck.Status;
import com.dota2coach.analysis.evolution.XRayEvolution.Format;
import com.dota2coach.analysis.evolution.XRayEvolution.Trend;
import com.dota2coach.analysis.metric.MatchAnalysis;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathLine;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.ProfileXRay.DeathPhase;
import com.dota2coach.analysis.metric.ProfileXRay.HeroBenchmarkLine;
import com.dota2coach.analysis.metric.ProfileXRay.HeroLaning;
import com.dota2coach.analysis.metric.ProfileXRay.HeroLine;
import com.dota2coach.analysis.metric.ProfileXRay.MetricComparison;
import com.dota2coach.analysis.metric.ProfileXRay.StatBlock;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.FocusMetric;
import com.dota2coach.core.model.FocusGoal;
import java.util.List;

/** Confere as metas dos focos com os números do Raio-X ou de uma partida, sem depender do texto da IA. */
public final class FocusGoalChecker {

    private FocusGoalChecker() {}

    /** Metas do diagnóstico anterior contra o Raio-X novo: atingiu, avançou, ficou igual ou piorou. */
    public static List<FocusGoalCheck> checkXRay(ProfileXRay current, List<FocusGoal> goals) {
        return goals.stream().map(g -> {
            Double now = xRayValue(current, g.getMetric(), g.getHeroName());
            return new FocusGoalCheck(g.getOrdinal(), g.getTitle(), g.getMetric(), g.getHeroName(), g.getBaseline(),
                    g.getTarget(), now, xRayStatus(g, now));
        }).toList();
    }

    /** Metas atuais contra uma partida: atingiu ou não; meta de outro herói não se aplica. */
    public static List<FocusGoalCheck> checkMatch(MatchAnalysis match, List<FocusGoal> goals) {
        return goals.stream().map(g -> {
            boolean otherHero = g.getHeroName() != null && !g.getHeroName().equalsIgnoreCase(match.heroName());
            Double now = otherHero ? null : matchValue(match, g.getMetric());
            Status status = otherHero || g.getMetric() == FocusMetric.WIN_RATE ? Status.NOT_APPLICABLE
                    : now == null ? Status.NO_DATA
                    : reached(g.getMetric(), now, g.getTarget()) ? Status.REACHED : Status.MISSED;
            return new FocusGoalCheck(g.getOrdinal(), g.getTitle(), g.getMetric(), g.getHeroName(), g.getBaseline(),
                    g.getTarget(), now, status);
        }).toList();
    }

    private static Status xRayStatus(FocusGoal g, Double now) {
        if (now == null) {
            return Status.NO_DATA;
        }
        if (reached(g.getMetric(), now, g.getTarget())) {
            return Status.REACHED;
        }
        if (g.getBaseline() == null) {
            return Status.MISSED;
        }
        Trend trend = XRayEvolutionCalculator.trend(g.getBaseline(), now, format(g.getMetric()),
                g.getMetric().higherIsBetter());
        return switch (trend) {
            case IMPROVED -> Status.ADVANCED;
            case WORSE -> Status.WORSE;
            default -> Status.SAME;
        };
    }

    static boolean reached(FocusMetric metric, double value, double target) {
        return metric.higherIsBetter() ? value >= target : value <= target;
    }

    static Format format(FocusMetric metric) {
        return switch (metric.unit()) {
            case PERCENT -> Format.PERCENT;
            case SECONDS -> Format.SECONDS;
            case NUMBER -> Format.DECIMAL;
        };
    }

    /** Valor da métrica no Raio-X; com herói, a média do jogador com ele quando o Raio-X tem esse recorte. */
    public static Double xRayValue(ProfileXRay x, FocusMetric metric, String hero) {
        if (hero != null) {
            Double byHero = heroValue(x, metric, hero);
            if (byHero != null || heroSpecific(metric)) {
                return byHero;
            }
        }
        StatBlock latest = x.fundamentals().latest();
        ProfileXRay.MatchDetails d = x.details();
        boolean hasLatest = latest.matches() > 0;
        boolean hasDetails = d.analyzedMatches() > 0;
        return switch (metric) {
            case WIN_RATE -> x.overview().recent().games() == 0 ? null : x.overview().recent().winRate();
            case GOLD_PER_MIN -> hasLatest ? latest.goldPerMin() : null;
            case XP_PER_MIN -> hasLatest ? latest.xpPerMin() : null;
            case LAST_HITS_PER_MIN -> hasLatest ? latest.lastHitsPerMin() : null;
            case HERO_DAMAGE_PER_MIN -> hasLatest ? latest.heroDamagePerMin() : null;
            case DEATHS_PER_MATCH -> hasLatest ? latest.deaths() : null;
            case KDA -> hasLatest ? latest.kda() : null;
            case LAST_HITS_AT_10 -> d.lastHitsAt10();
            case DENIES_AT_10 -> d.deniesAt10();
            case SECONDS_DEAD -> hasDetails ? d.secondsDeadPerMatch() : null;
            case DEATHS_0_10, DEATHS_10_20, DEATHS_20_30, DEATHS_30_PLUS -> !hasDetails ? null
                    : d.deathsByPhase().stream().filter(p -> p.label().equals(metric.deathPhase()))
                            .map(DeathPhase::deathsPerMatch).findFirst().orElse(null);
        };
    }

    private static boolean heroSpecific(FocusMetric metric) {
        return switch (metric) {
            case GOLD_PER_MIN, XP_PER_MIN, LAST_HITS_PER_MIN, HERO_DAMAGE_PER_MIN, LAST_HITS_AT_10 -> true;
            default -> false;
        };
    }

    private static Double heroValue(ProfileXRay x, FocusMetric metric, String hero) {
        HeroLine line = x.heroes().stream().filter(h -> h.name().equalsIgnoreCase(hero)).findFirst().orElse(null);
        return switch (metric) {
            case GOLD_PER_MIN -> line == null || line.recent().games() == 0 ? null : line.goldPerMin();
            case XP_PER_MIN -> line == null || line.recent().games() == 0 ? null : line.xpPerMin();
            case LAST_HITS_PER_MIN -> benchmark(x, hero, BenchmarkMetric.LAST_HITS_PER_MIN);
            case HERO_DAMAGE_PER_MIN -> benchmark(x, hero, BenchmarkMetric.HERO_DAMAGE_PER_MIN);
            case LAST_HITS_AT_10 -> x.details().laningByHero().stream()
                    .filter(h -> h.heroName().equalsIgnoreCase(hero)).map(HeroLaning::lastHitsAt10)
                    .findFirst().orElse(null);
            default -> null;
        };
    }

    private static Double benchmark(ProfileXRay x, String hero, BenchmarkMetric metric) {
        return x.benchmarks().stream().filter(b -> b.name().equalsIgnoreCase(hero))
                .map(HeroBenchmarkLine::metrics).map(m -> m.get(metric)).filter(java.util.Objects::nonNull)
                .map(MetricComparison::playerValue).findFirst().orElse(null);
    }

    /** Valor da métrica numa partida; mortes por fase, tempo morto e rota só com replay analisado. */
    public static Double matchValue(MatchAnalysis a, FocusMetric metric) {
        double minutes = a.duration().toSeconds() / 60.0;
        MatchAnalysis.Line line = a.line();
        return switch (metric) {
            case WIN_RATE -> null;
            case GOLD_PER_MIN -> (double) line.goldPerMin();
            case XP_PER_MIN -> (double) line.xpPerMin();
            case LAST_HITS_PER_MIN -> minutes <= 0 ? null : line.lastHits() / minutes;
            case HERO_DAMAGE_PER_MIN -> minutes <= 0 ? null : line.heroDamage() / minutes;
            case DEATHS_PER_MATCH -> (double) line.deaths();
            case KDA -> (line.kills() + line.assists()) / (double) Math.max(1, line.deaths());
            case LAST_HITS_AT_10 -> a.laning().lastHitsAt10() == null ? null : a.laning().lastHitsAt10().doubleValue();
            case DENIES_AT_10 -> a.laning().deniesAt10() == null ? null : a.laning().deniesAt10().doubleValue();
            case SECONDS_DEAD -> !a.replayParsed() ? null
                    : (double) a.deaths().stream().mapToInt(DeathLine::secondsDead).sum();
            case DEATHS_0_10, DEATHS_10_20, DEATHS_20_30, DEATHS_30_PLUS -> !a.replayParsed() ? null
                    : a.deathsByPhase().stream().filter(p -> p.phase().equals(metric.deathPhase()))
                            .map(p -> (double) p.deaths()).findFirst().orElse(0.0);
        };
    }
}
