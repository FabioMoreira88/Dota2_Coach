package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.ProfileXRay.DeathPhase;
import com.dota2coach.analysis.metric.ProfileXRay.HeroLaning;
import com.dota2coach.analysis.metric.ProfileXRay.ItemTiming;
import com.dota2coach.analysis.metric.ProfileXRay.MatchDetails;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.ProfileSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fase de rotas, mortes por fase e tempo dos itens, a partir das partidas com replay analisado. */
final class MatchDetailsCalculator {

    /** Limites das fases, em minutos: 0-10, 10-20, 20-30 e 30 ou mais. */
    static final int[] PHASE_LIMITS_MIN = {10, 20, 30};
    /** Partidas detalhadas mínimas com o herói para mostrar fase de rotas e tempo de itens. */
    static final int MIN_HERO_MATCHES = 3;
    /** Item precisa ter sido comprado em pelo menos esta fração das partidas detalhadas do herói. */
    static final double MIN_ITEM_SHARE = 0.4;
    static final int ITEMS_PER_HERO = 4;

    private MatchDetailsCalculator() {}

    static MatchDetails calculate(ProfileSnapshot snapshot, List<MatchRecord> recent) {
        List<MatchDetail> details = recent.stream()
                .map(m -> snapshot.matchDetails().get(m.matchId()))
                .filter(Objects::nonNull)
                .toList();
        if (details.isEmpty()) {
            return MatchDetails.EMPTY;
        }
        return new MatchDetails(
                details.size(),
                averageOf(details, MatchDetail::lastHitsAt10),
                averageOf(details, MatchDetail::deniesAt10),
                laningByHero(snapshot, details),
                deathsByPhase(details),
                details.stream().flatMap(d -> d.deaths().stream()).mapToInt(MatchDetail.Death::secondsDead).sum()
                        / (double) details.size(),
                itemTimings(snapshot, details));
    }

    private static Double averageOf(List<MatchDetail> details, Function<MatchDetail, Integer> value) {
        var values = details.stream().map(value).filter(Objects::nonNull).mapToInt(Integer::intValue);
        var avg = values.average();
        return avg.isPresent() ? avg.getAsDouble() : null;
    }

    static List<HeroLaning> laningByHero(ProfileSnapshot snapshot, List<MatchDetail> details) {
        return byHero(details).entrySet().stream()
                .map(e -> {
                    Double lh = averageOf(e.getValue(), MatchDetail::lastHitsAt10);
                    long known = e.getValue().stream().filter(d -> d.lastHitsAt10() != null).count();
                    return lh == null || known < MIN_HERO_MATCHES ? null
                            : new HeroLaning(snapshot.heroName(e.getKey()), (int) known, lh);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(HeroLaning::matches).reversed())
                .toList();
    }

    static List<DeathPhase> deathsByPhase(List<MatchDetail> details) {
        int[] counts = new int[PHASE_LIMITS_MIN.length + 1];
        for (MatchDetail d : details) {
            for (MatchDetail.Death death : d.deaths()) {
                counts[phaseIndex(death.timeSeconds())]++;
            }
        }
        int total = java.util.Arrays.stream(counts).sum();
        List<DeathPhase> phases = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            phases.add(new DeathPhase(phaseLabel(i), counts[i] / (double) details.size(),
                    total == 0 ? 0.0 : counts[i] / (double) total));
        }
        return phases;
    }

    static int phaseIndex(int timeSeconds) {
        for (int i = 0; i < PHASE_LIMITS_MIN.length; i++) {
            if (timeSeconds < PHASE_LIMITS_MIN[i] * 60) {
                return i;
            }
        }
        return PHASE_LIMITS_MIN.length;
    }

    static String phaseLabel(int index) {
        if (index == 0) {
            return "0 a " + PHASE_LIMITS_MIN[0] + " min";
        }
        if (index == PHASE_LIMITS_MIN.length) {
            return PHASE_LIMITS_MIN[index - 1] + " min ou mais";
        }
        return PHASE_LIMITS_MIN[index - 1] + " a " + PHASE_LIMITS_MIN[index] + " min";
    }

    static List<ItemTiming> itemTimings(ProfileSnapshot snapshot, List<MatchDetail> details) {
        Map<String, Item> byKey = snapshot.items().values().stream()
                .collect(Collectors.toMap(Item::key, Function.identity(), (a, b) -> a));
        List<ItemTiming> timings = new ArrayList<>();
        byHero(details).forEach((heroId, games) -> {
            if (games.size() < MIN_HERO_MATCHES) {
                return;
            }
            Map<String, List<Integer>> firstPurchase = new HashMap<>();
            for (MatchDetail d : games) {
                Map<String, Integer> first = new LinkedHashMap<>();
                d.purchases().forEach(p -> first.putIfAbsent(p.itemKey(), p.timeSeconds()));
                first.forEach((key, time) -> {
                    Item item = byKey.get(key);
                    if (item != null && XRayCalculator.isCoreItem(item)) {
                        firstPurchase.computeIfAbsent(key, k -> new ArrayList<>()).add(time);
                    }
                });
            }
            firstPurchase.entrySet().stream()
                    .filter(e -> e.getValue().size() >= MIN_ITEM_SHARE * games.size())
                    .sorted(Comparator.comparingInt((Map.Entry<String, List<Integer>> e) -> e.getValue().size())
                            .reversed().thenComparing(Map.Entry::getKey))
                    .limit(ITEMS_PER_HERO)
                    .map(e -> new ItemTiming(snapshot.heroName(heroId), byKey.get(e.getKey()).name(),
                            e.getValue().size(), median(e.getValue()) / 60.0))
                    .sorted(Comparator.comparingDouble(ItemTiming::medianMinute))
                    .forEach(timings::add);
        });
        return timings;
    }

    private static Map<Integer, List<MatchDetail>> byHero(List<MatchDetail> details) {
        Map<Integer, List<MatchDetail>> byHero = details.stream()
                .collect(Collectors.groupingBy(MatchDetail::heroId, LinkedHashMap::new, Collectors.toList()));
        return byHero.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Integer, List<MatchDetail>> e) -> e.getValue().size())
                        .reversed().thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    static double median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
