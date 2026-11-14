package com.dota2coach.collector.mapper;

import com.dota2coach.collector.dto.opendota.OpenDotaAbilityConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaBenchmarksResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroDurationResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroMatchupResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroStatsResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaItemConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaItemPopularityResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaItemTimingResponse;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.DamageType;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.model.Hero;
import com.dota2coach.core.model.HeroBenchmark;
import com.dota2coach.core.model.HeroDurations;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.HeroItemTimings;
import com.dota2coach.core.model.HeroMatchups;
import com.dota2coach.core.model.HeroMeta.PickWin;
import com.dota2coach.core.model.HeroMeta;
import com.dota2coach.core.model.Item;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                        Boolean.TRUE.equals(e.getValue().created()),
                        e.getValue().components() == null ? List.of()
                                : e.getValue().components().stream().filter(Objects::nonNull).toList()))
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

    /** Tipo de dano de cada habilidade. Quando a OpenDota manda uma lista, vale o primeiro tipo. */
    public static Map<String, DamageType> toDamageTypes(Map<String, OpenDotaAbilityConstant> abilities) {
        Map<String, DamageType> types = new HashMap<>();
        abilities.forEach((key, ability) -> {
            if (key == null || ability == null) {
                return;
            }
            Object raw = ability.damageType();
            if (raw instanceof List<?> list) {
                raw = list.isEmpty() ? null : list.get(0);
            }
            DamageType type = DamageType.fromOpenDota(raw == null ? null : raw.toString());
            if (type != DamageType.OTHER) {
                types.put(key, type);
            }
        });
        return types;
    }

    public static HeroDurations toHeroDurations(int heroId, List<OpenDotaHeroDurationResponse> durations) {
        return new HeroDurations(heroId, durations.stream()
                .filter(d -> d != null && d.durationBin() != null && d.gamesPlayed() != null && d.wins() != null)
                .map(d -> new HeroDurations.Bin(d.durationBin(), d.gamesPlayed(), d.wins()))
                .sorted(Comparator.comparingInt(HeroDurations.Bin::fromSeconds))
                .toList());
    }

    /** Tempos de item do herói. Linhas de outro herói ou incompletas ficam de fora. */
    public static HeroItemTimings toItemTimings(int heroId, List<OpenDotaItemTimingResponse> timings) {
        Map<String, List<HeroItemTimings.Bin>> byItem = new HashMap<>();
        timings.stream()
                .filter(t -> t != null && t.item() != null && t.time() != null && t.games() != null
                        && t.wins() != null && Objects.equals(t.heroId(), heroId))
                .forEach(t -> byItem.computeIfAbsent(t.item(), k -> new ArrayList<>())
                        .add(new HeroItemTimings.Bin(t.time(), t.games(), t.wins())));
        return new HeroItemTimings(heroId, byItem);
    }

    public static HeroMatchups toHeroMatchups(int heroId, List<OpenDotaHeroMatchupResponse> matchups) {
        Map<Integer, HeroMatchups.Record> against = new HashMap<>();
        matchups.stream()
                .filter(m -> m != null && m.heroId() != null && m.gamesPlayed() != null && m.wins() != null)
                .forEach(m -> against.put(m.heroId(), new HeroMatchups.Record(m.gamesPlayed(), m.wins())));
        return new HeroMatchups(heroId, against);
    }
}
