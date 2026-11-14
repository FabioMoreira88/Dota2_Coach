package com.dota2coach.collector.mapper;

import com.dota2coach.collector.dto.opendota.OpenDotaBenchmarksResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroStatsResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaItemConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaItemPopularityResponse;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.model.Hero;
import com.dota2coach.core.model.HeroBenchmark;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.HeroMeta;
import com.dota2coach.core.model.HeroMeta.PickWin;
import com.dota2coach.core.model.Item;
import java.util.Collection;
import java.util.Objects;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Converte dados de referência da OpenDota (heróis, itens, meta) no modelo do domínio. */
public final class OpenDotaReferenceMapper {

    private OpenDotaReferenceMapper() {}

    public static Map<Integer, Hero> toHeroes(Collection<OpenDotaHeroConstant> heroes) {
        return heroes.stream()
                .map(h -> new Hero(h.id(), h.localizedName(), h.primaryAttr(),
                        h.roles() == null ? List.of() : h.roles()))
                .collect(Collectors.toMap(Hero::id, Function.identity(), (a, b) -> a));
    }

    /** Itens sem id (entradas internas da Valve) são ignorados. */
    public static Map<Integer, Item> toItems(Map<String, OpenDotaItemConstant> items) {
        return items.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().id() > 0)
                .map(e -> new Item(
                        e.getValue().id(),
                        e.getKey(),
                        e.getValue().displayName() == null ? e.getKey() : e.getValue().displayName(),
                        e.getValue().cost() == null ? 0 : e.getValue().cost(),
                        Boolean.TRUE.equals(e.getValue().created())))
                .collect(Collectors.toMap(Item::id, Function.identity(), (a, b) -> a));
    }

    public static Map<Integer, HeroMeta> toHeroMeta(List<OpenDotaHeroStatsResponse> stats) {
        return stats.stream()
                .map(OpenDotaReferenceMapper::toHeroMeta)
                .collect(Collectors.toMap(HeroMeta::heroId, Function.identity(), (a, b) -> a));
    }

    static HeroMeta toHeroMeta(OpenDotaHeroStatsResponse s) {
        Map<RankBracket, PickWin> byBracket = new EnumMap<>(RankBracket.class);
        for (RankBracket bracket : RankBracket.values()) {
            byBracket.put(bracket, new PickWin(
                    s.bracketPicks().getOrDefault(bracket.tier(), 0),
                    s.bracketWins().getOrDefault(bracket.tier(), 0)));
        }
        return new HeroMeta(s.id(), byBracket, new PickWin(s.proPick(), s.proWin()));
    }

    public static HeroItemPopularity toItemPopularity(int heroId, OpenDotaItemPopularityResponse r) {
        Map<GamePhase, Map<Integer, Integer>> byPhase = new EnumMap<>(GamePhase.class);
        if (r != null) {
            putIfPresent(byPhase, GamePhase.START, r.startGameItems());
            putIfPresent(byPhase, GamePhase.EARLY, r.earlyGameItems());
            putIfPresent(byPhase, GamePhase.MID, r.midGameItems());
            putIfPresent(byPhase, GamePhase.LATE, r.lateGameItems());
        }
        return new HeroItemPopularity(heroId, byPhase);
    }

    private static void putIfPresent(
            Map<GamePhase, Map<Integer, Integer>> byPhase, GamePhase phase, Map<Integer, Integer> items) {
        if (items != null) {
            byPhase.put(phase, items);
        }
    }

    /** Métricas sem dados ou pontos sem valor são ignorados. */
    public static HeroBenchmark toHeroBenchmark(int heroId, OpenDotaBenchmarksResponse r) {
        Map<BenchmarkMetric, List<HeroBenchmark.Point>> byMetric = new EnumMap<>(BenchmarkMetric.class);
        if (r != null && r.result() != null) {
            for (BenchmarkMetric metric : BenchmarkMetric.values()) {
                List<OpenDotaBenchmarksResponse.Point> points = r.result().get(metric.openDotaKey());
                if (points == null) {
                    continue;
                }
                List<HeroBenchmark.Point> mapped = points.stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p.value() != null)
                        .map(p -> new HeroBenchmark.Point(p.percentile(), p.value()))
                        .toList();
                if (!mapped.isEmpty()) {
                    byMetric.put(metric, mapped);
                }
            }
        }
        return new HeroBenchmark(heroId, byMetric);
    }
}
