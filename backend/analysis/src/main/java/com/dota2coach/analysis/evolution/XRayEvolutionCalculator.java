package com.dota2coach.analysis.evolution;

import com.dota2coach.analysis.evolution.XRayEvolution.Change;
import com.dota2coach.analysis.evolution.XRayEvolution.Format;
import com.dota2coach.analysis.evolution.XRayEvolution.Trend;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.ProfileXRay.DeathPhase;
import com.dota2coach.analysis.metric.ProfileXRay.HeroBenchmarkLine;
import com.dota2coach.analysis.metric.ProfileXRay.MatchDetails;
import com.dota2coach.analysis.metric.ProfileXRay.MetricComparison;
import com.dota2coach.analysis.metric.ProfileXRay.StatBlock;
import com.dota2coach.core.enums.BenchmarkMetric;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Compara o Raio-X atual com o anterior da mesma conta, métrica a métrica. */
public final class XRayEvolutionCalculator {

    /** Variação relativa abaixo da qual a métrica conta como igual. */
    static final double RELATIVE_TOLERANCE = 0.05;
    /** Heróis comparados: os mais jogados que aparecem nos dois Raio-X. */
    static final int MAX_HEROES = 3;

    private XRayEvolutionCalculator() {}

    public static XRayEvolution compare(ProfileXRay previous, ProfileXRay current) {
        List<Change> changes = new ArrayList<>();

        String recent = "Últimas " + current.recentWindow() + " partidas";
        changes.add(change(recent, "Taxa de vitória", previous.overview().recent().games() == 0 ? null
                : previous.overview().recent().winRate(), current.overview().recent().games() == 0 ? null
                : current.overview().recent().winRate(), Format.PERCENT, true));

        StatBlock before = previous.fundamentals().latest();
        StatBlock after = current.fundamentals().latest();
        String fundamentals = "Fundamentos (últimas " + after.matches() + " partidas)";
        stat(changes, fundamentals, "Ouro por minuto", before, after, StatBlock::goldPerMin, true);
        stat(changes, fundamentals, "XP por minuto", before, after, StatBlock::xpPerMin, true);
        stat(changes, fundamentals, "Last hits por minuto", before, after, StatBlock::lastHitsPerMin, true);
        stat(changes, fundamentals, "Mortes por partida", before, after, StatBlock::deaths, false);
        stat(changes, fundamentals, "KDA", before, after, StatBlock::kda, true);
        stat(changes, fundamentals, "Dano em heróis por minuto", before, after, StatBlock::heroDamagePerMin, true);

        MatchDetails db = previous.details();
        MatchDetails da = current.details();
        String details = "Partidas com replay analisado";
        changes.add(change(details, "Last hits aos 10 min", db.lastHitsAt10(), da.lastHitsAt10(), Format.DECIMAL,
                true));
        changes.add(change(details, "Denies aos 10 min", db.deniesAt10(), da.deniesAt10(), Format.DECIMAL, true));
        Map<String, Double> phasesBefore = phases(db);
        for (DeathPhase phase : da.deathsByPhase()) {
            changes.add(change(details, "Mortes de " + phase.label(),
                    db.analyzedMatches() == 0 ? null : phasesBefore.get(phase.label()),
                    da.analyzedMatches() == 0 ? null : phase.deathsPerMatch(), Format.DECIMAL, false));
        }
        changes.add(change(details, "Tempo morto por partida",
                db.analyzedMatches() == 0 ? null : db.secondsDeadPerMatch(),
                da.analyzedMatches() == 0 ? null : da.secondsDeadPerMatch(), Format.SECONDS, false));

        heroes(changes, previous, current);

        return new XRayEvolution(previous.collectedAt(), previous.mainPosition(),
                Math.max(0, current.overview().allTime().games() - previous.overview().allTime().games()),
                current.recentWindow(), changes);
    }

    private static void heroes(List<Change> changes, ProfileXRay previous, ProfileXRay current) {
        Map<Integer, HeroBenchmarkLine> before = previous.benchmarks().stream()
                .collect(Collectors.toMap(HeroBenchmarkLine::heroId, Function.identity(), (a, b) -> a));
        current.benchmarks().stream()
                .filter(h -> before.containsKey(h.heroId()))
                .sorted(Comparator.comparingInt(HeroBenchmarkLine::games).reversed())
                .limit(MAX_HEROES)
                .forEach(h -> {
                    HeroBenchmarkLine old = before.get(h.heroId());
                    for (BenchmarkMetric metric : BenchmarkMetric.values()) {
                        MetricComparison a = old.metrics().get(metric);
                        MetricComparison b = h.metrics().get(metric);
                        if (a != null && b != null) {
                            changes.add(change(h.name() + " (média do jogador)", metric.label(), a.playerValue(),
                                    b.playerValue(), Format.DECIMAL, metric.higherIsBetter()));
                        }
                    }
                });
    }

    private static Map<String, Double> phases(MatchDetails details) {
        return details.deathsByPhase().stream()
                .collect(Collectors.toMap(DeathPhase::label, DeathPhase::deathsPerMatch, (a, b) -> a));
    }

    private static void stat(List<Change> changes, String group, String label, StatBlock before, StatBlock after,
            ToDoubleFunction<StatBlock> value, boolean higherIsBetter) {
        changes.add(change(group, label, before.matches() == 0 ? null : value.applyAsDouble(before),
                after.matches() == 0 ? null : value.applyAsDouble(after), Format.DECIMAL, higherIsBetter));
    }

    static Change change(String group, String label, Double before, Double after, Format format,
            boolean higherIsBetter) {
        return new Change(group, label, before, after, format, trend(before, after, format, higherIsBetter));
    }

    /** Igual quando a variação fica dentro de 5% do valor anterior ou de um mínimo absoluto por formato. */
    static Trend trend(Double before, Double after, Format format, boolean higherIsBetter) {
        if (before == null || after == null) {
            return Trend.NO_DATA;
        }
        double minimum = switch (format) {
            case PERCENT -> 0.02;
            case SECONDS -> 10;
            case DECIMAL -> 0.05;
        };
        double delta = after - before;
        if (Math.abs(delta) <= Math.max(minimum, Math.abs(before) * RELATIVE_TOLERANCE)) {
            return Trend.SAME;
        }
        return (delta > 0) == higherIsBetter ? Trend.IMPROVED : Trend.WORSE;
    }
}
