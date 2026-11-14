package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.ProfileXRay.Fundamentals;
import com.dota2coach.analysis.metric.ProfileXRay.HeroBenchmarkLine;
import com.dota2coach.analysis.metric.ProfileXRay.HeroItems;
import com.dota2coach.analysis.metric.ProfileXRay.HeroLine;
import com.dota2coach.analysis.metric.ProfileXRay.ItemUse;
import com.dota2coach.analysis.metric.ProfileXRay.LaneLine;
import com.dota2coach.analysis.metric.ProfileXRay.MetricComparison;
import com.dota2coach.analysis.metric.ProfileXRay.Overview;
import com.dota2coach.analysis.metric.ProfileXRay.StatBlock;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.model.HeroBenchmark;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.HeroMeta;
import com.dota2coach.core.model.HeroMeta.PickWin;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.ProfileSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Calcula o Raio-X a partir dos dados coletados. Não faz I/O e não depende de Spring. */
public class XRayCalculator {

    /** Partidas que formam o recorte "recente". */
    public static final int RECENT_WINDOW = 100;
    /** Partidas mais recentes comparadas com o resto do recorte nos fundamentos. */
    public static final int LATEST_WINDOW = 20;
    /** Partidas mínimas no recorte recente para o herói contar no pool. */
    public static final int POOL_MIN_GAMES = 5;
    static final int TOP_HEROES = 10;
    static final int ITEM_HEROES = 5;
    /** Partidas do herói (com itens) usadas na análise de itens. */
    static final int ITEM_MATCHES_PER_HERO = 50;
    static final int MIN_MATCHES_FOR_ITEMS = 5;
    /** Itens abaixo deste custo (consumíveis, sentinelas, componentes baratos) ficam fora da análise. */
    public static final int CORE_ITEM_MIN_COST = 1000;
    static final int PLAYER_TOP_ITEMS = 6;
    static final int META_ITEMS_PER_PHASE = 8;
    /** Item popular é "faltante" quando aparece em menos que esta fração das partidas do jogador com o herói. */
    static final double MISSING_META_MAX_SHARE = 0.10;
    static final int MISSING_META_CANDIDATES = 5;
    private static final Duration LAST_30_DAYS = Duration.ofDays(30);
    /** Itens básicos (sem receita) que normalmente são mantidos como item final. */
    static final Set<String> STANDALONE_FINAL_ITEMS = Set.of("blink");

    public ProfileXRay calculate(ProfileSnapshot snapshot) {
        return calculate(snapshot, null);
    }

    /**
     * @param mainPosition posição que o jogador informou; nulo quando ele não informou
     */
    public ProfileXRay calculate(ProfileSnapshot snapshot, Position mainPosition) {
        List<MatchRecord> all = snapshot.matches().stream()
                .sorted(Comparator.comparing(MatchRecord::startTime).reversed())
                .toList();
        List<MatchRecord> recent = all.stream().limit(RECENT_WINDOW).toList();
        RankBracket bracket = RankBracket.fromRankTier(snapshot.player().rankTier()).orElse(null);
        MetaView meta = new MetaView(snapshot.heroMeta(), bracket);

        List<HeroLine> heroes = heroLines(snapshot, all, recent, meta);
        return new ProfileXRay(
                snapshot.player().personaName(),
                snapshot.player().rankTier(),
                bracket,
                snapshot.collectedAt(),
                mainPosition,
                RECENT_WINDOW,
                overview(all, recent, snapshot.collectedAt()),
                heroes,
                heroPoolSize(recent),
                lanes(recent),
                laneCoverage(recent),
                fundamentals(recent),
                benchmarks(snapshot, recent, heroes),
                items(snapshot, all, heroes),
                shareOnWinningMetaHeroes(recent, meta));
    }

    static Overview overview(List<MatchRecord> all, List<MatchRecord> recent, Instant now) {
        Instant cutoff = now.minus(LAST_30_DAYS);
        return new Overview(
                winLoss(all),
                winLoss(recent),
                (int) recent.stream().filter(MatchRecord::ranked).count(),
                (int) recent.stream().filter(MatchRecord::abandoned).count(),
                (int) recent.stream().filter(m -> m.partySize() > 1).count(),
                (int) all.stream().filter(m -> !m.startTime().isBefore(cutoff)).count(),
                all.isEmpty() ? null : all.get(all.size() - 1).startTime(),
                all.isEmpty() ? null : all.get(0).startTime());
    }

    private static List<HeroLine> heroLines(
            ProfileSnapshot snapshot, List<MatchRecord> all, List<MatchRecord> recent, MetaView meta) {
        Map<Integer, List<MatchRecord>> recentByHero = recent.stream()
                .collect(Collectors.groupingBy(MatchRecord::heroId));
        Map<Integer, WinLoss> allTimeByHero = all.stream()
                .collect(Collectors.groupingBy(MatchRecord::heroId,
                        Collectors.reducing(WinLoss.EMPTY, m -> new WinLoss(1, m.won() ? 1 : 0),
                                (a, b) -> new WinLoss(a.games() + b.games(), a.wins() + b.wins()))));
        return recentByHero.entrySet().stream()
                .map(e -> {
                    List<MatchRecord> games = e.getValue();
                    int heroId = e.getKey();
                    return new HeroLine(
                            heroId,
                            snapshot.heroName(heroId),
                            winLoss(games),
                            allTimeByHero.getOrDefault(heroId, WinLoss.EMPTY),
                            average(games, MatchRecord::kda),
                            average(games, MatchRecord::goldPerMin),
                            average(games, MatchRecord::xpPerMin),
                            meta.winRate(heroId),
                            meta.pickRate(heroId));
                })
                .sorted(Comparator.comparingInt((HeroLine h) -> h.recent().games()).reversed()
                        .thenComparing(Comparator.comparingInt((HeroLine h) -> h.allTime().games()).reversed())
                        .thenComparingInt(HeroLine::heroId))
                .limit(TOP_HEROES)
                .toList();
    }

    static int heroPoolSize(List<MatchRecord> recent) {
        return (int) recent.stream()
                .collect(Collectors.groupingBy(MatchRecord::heroId, Collectors.counting()))
                .values().stream()
                .filter(games -> games >= POOL_MIN_GAMES)
                .count();
    }

    static List<LaneLine> lanes(List<MatchRecord> recent) {
        Map<Lane, WinLoss> byLane = new EnumMap<>(Lane.class);
        recent.stream()
                .filter(m -> m.lane() != Lane.UNKNOWN)
                .forEach(m -> byLane.merge(m.lane(), new WinLoss(1, m.won() ? 1 : 0),
                        (a, b) -> new WinLoss(a.games() + b.games(), a.wins() + b.wins())));
        return byLane.entrySet().stream()
                .map(e -> new LaneLine(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt((LaneLine l) -> l.result().games()).reversed())
                .toList();
    }

    static double laneCoverage(List<MatchRecord> recent) {
        return recent.isEmpty() ? 0.0
                : (double) recent.stream().filter(m -> m.lane() != Lane.UNKNOWN).count() / recent.size();
    }

    static Fundamentals fundamentals(List<MatchRecord> recent) {
        List<MatchRecord> latest = recent.stream().limit(LATEST_WINDOW).toList();
        List<MatchRecord> previous = recent.stream().skip(LATEST_WINDOW).toList();
        return new Fundamentals(statBlock(latest), statBlock(previous));
    }

    static StatBlock statBlock(List<MatchRecord> matches) {
        if (matches.isEmpty()) {
            return StatBlock.EMPTY;
        }
        return new StatBlock(
                matches.size(),
                winLoss(matches).winRate(),
                average(matches, MatchRecord::goldPerMin),
                average(matches, MatchRecord::xpPerMin),
                average(matches, m -> perMinute(m.lastHits(), m)),
                average(matches, MatchRecord::deaths),
                average(matches, MatchRecord::kda),
                average(matches, m -> perMinute(m.heroDamage(), m)),
                average(matches, MatchRecord::durationMinutes));
    }

    static List<HeroBenchmarkLine> benchmarks(
            ProfileSnapshot snapshot, List<MatchRecord> recent, List<HeroLine> heroes) {
        return heroes.stream()
                .filter(h -> h.recent().games() >= POOL_MIN_GAMES && snapshot.benchmarks().containsKey(h.heroId()))
                .limit(ITEM_HEROES)
                .map(h -> benchmarkLine(snapshot.benchmarks().get(h.heroId()), h,
                        recent.stream().filter(m -> m.heroId() == h.heroId()).toList()))
                .filter(line -> !line.metrics().isEmpty())
                .toList();
    }

    static HeroBenchmarkLine benchmarkLine(HeroBenchmark benchmark, HeroLine hero, List<MatchRecord> games) {
        Map<BenchmarkMetric, MetricComparison> metrics = new EnumMap<>(BenchmarkMetric.class);
        for (BenchmarkMetric metric : BenchmarkMetric.values()) {
            double value = average(games, m -> metricValue(metric, m));
            benchmark.percentileOf(metric, value)
                    .ifPresent(p -> metrics.put(metric, new MetricComparison(value, p)));
        }
        return new HeroBenchmarkLine(hero.heroId(), hero.name(), games.size(), metrics);
    }

    static double metricValue(BenchmarkMetric metric, MatchRecord m) {
        return switch (metric) {
            case GOLD_PER_MIN -> m.goldPerMin();
            case XP_PER_MIN -> m.xpPerMin();
            case LAST_HITS_PER_MIN -> perMinute(m.lastHits(), m);
            case HERO_DAMAGE_PER_MIN -> perMinute(m.heroDamage(), m);
            case DEATHS_PER_MIN -> perMinute(m.deaths(), m);
        };
    }

    private static List<HeroItems> items(ProfileSnapshot snapshot, List<MatchRecord> all, List<HeroLine> heroes) {
        return heroes.stream()
                .filter(h -> snapshot.itemPopularity().containsKey(h.heroId()))
                .limit(ITEM_HEROES)
                .map(h -> heroItems(snapshot, all, h))
                .filter(hi -> hi.matchesWithItems() >= MIN_MATCHES_FOR_ITEMS)
                .toList();
    }

    static HeroItems heroItems(ProfileSnapshot snapshot, List<MatchRecord> all, HeroLine hero) {
        Map<Integer, Item> catalog = snapshot.items();
        List<MatchRecord> games = all.stream()
                .filter(m -> m.heroId() == hero.heroId() && !m.items().isEmpty())
                .limit(ITEM_MATCHES_PER_HERO)
                .toList();
        Map<Integer, Integer> counts = new HashMap<>();
        for (MatchRecord m : games) {
            m.items().stream()
                    .distinct()
                    .filter(id -> isCoreItem(catalog.get(id)))
                    .forEach(id -> counts.merge(id, 1, Integer::sum));
        }

        HeroItemPopularity popularity = snapshot.itemPopularity().get(hero.heroId());
        Set<Integer> metaItems = new LinkedHashSet<>();
        for (GamePhase phase : List.of(GamePhase.EARLY, GamePhase.MID, GamePhase.LATE)) {
            popularity.top(phase, Integer.MAX_VALUE).stream()
                    .filter(id -> isCoreItem(catalog.get(id)))
                    .limit(META_ITEMS_PER_PHASE)
                    .forEach(metaItems::add);
        }

        List<ItemUse> playerTop = counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(PLAYER_TOP_ITEMS)
                .map(e -> new ItemUse(e.getKey(), snapshot.itemName(e.getKey()), e.getValue(),
                        metaItems.contains(e.getKey())))
                .toList();
        double adherence = playerTop.isEmpty() ? 0.0
                : (double) playerTop.stream().filter(ItemUse::meta).count() / playerTop.size();

        List<Integer> metaCandidates = new LinkedHashSet<>(concat(
                topCore(popularity, GamePhase.MID, catalog), topCore(popularity, GamePhase.LATE, catalog)))
                .stream().toList();
        List<String> missing = metaCandidates.stream()
                .filter(id -> counts.getOrDefault(id, 0) < MISSING_META_MAX_SHARE * games.size())
                .map(snapshot::itemName)
                .toList();

        return new HeroItems(
                hero.heroId(),
                hero.name(),
                games.size(),
                playerTop,
                metaItems.stream().map(snapshot::itemName).toList(),
                adherence,
                missing);
    }

    private static List<Integer> topCore(HeroItemPopularity popularity, GamePhase phase, Map<Integer, Item> catalog) {
        return popularity.top(phase, Integer.MAX_VALUE).stream()
                .filter(id -> isCoreItem(catalog.get(id)))
                .limit(MISSING_META_CANDIDATES)
                .toList();
    }

    private static List<Integer> concat(List<Integer> a, List<Integer> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    /**
     * Item principal: custa {@link #CORE_ITEM_MIN_COST} ou mais e é montado (BKB, Treads) ou é um item básico que
     * costuma ficar no inventário até o fim (Blink). Componentes caros como Ogre Axe e Hyperstone ficam de fora,
     * porque a popularidade da OpenDota conta cada compra, inclusive de componentes. Item desconhecido no catálogo
     * conta como principal: melhor mostrar do que esconder.
     */
    static boolean isCoreItem(Item item) {
        if (item == null) {
            return true;
        }
        return item.cost() >= CORE_ITEM_MIN_COST
                && (item.assembled() || STANDALONE_FINAL_ITEMS.contains(item.key()));
    }

    static double shareOnWinningMetaHeroes(List<MatchRecord> recent, MetaView meta) {
        List<Double> rates = recent.stream().map(m -> meta.winRate(m.heroId())).toList();
        long known = rates.stream().filter(Objects::nonNull).count();
        if (known == 0) {
            return 0.0;
        }
        return (double) rates.stream().filter(r -> r != null && r >= 0.5).count() / known;
    }

    static WinLoss winLoss(List<MatchRecord> matches) {
        int wins = (int) matches.stream().filter(MatchRecord::won).count();
        return new WinLoss(matches.size(), wins);
    }

    private static double average(List<MatchRecord> matches, ToDoubleFunction<MatchRecord> value) {
        return matches.stream().mapToDouble(value).average().orElse(0.0);
    }

    private static double perMinute(int value, MatchRecord m) {
        double minutes = m.durationMinutes();
        return minutes <= 0 ? 0.0 : value / minutes;
    }

    /** Meta na faixa do jogador; sem medalha, soma todas as faixas. */
    static final class MetaView {

        /** Cada partida tem 10 escolhas. */
        private static final int PICKS_PER_MATCH = 10;

        private final Map<Integer, HeroMeta> heroMeta;
        private final RankBracket bracket;
        private final long totalPicks;

        MetaView(Map<Integer, HeroMeta> heroMeta, RankBracket bracket) {
            this.heroMeta = heroMeta;
            this.bracket = bracket;
            this.totalPicks = heroMeta.values().stream().mapToLong(h -> pickWin(h).picks()).sum();
        }

        Double winRate(int heroId) {
            HeroMeta meta = heroMeta.get(heroId);
            if (meta == null) {
                return null;
            }
            PickWin pw = pickWin(meta);
            return pw.picks() == 0 ? null : pw.winRate();
        }

        Double pickRate(int heroId) {
            HeroMeta meta = heroMeta.get(heroId);
            if (meta == null || totalPicks == 0) {
                return null;
            }
            return pickWin(meta).picks() / ((double) totalPicks / PICKS_PER_MATCH);
        }

        private PickWin pickWin(HeroMeta meta) {
            if (bracket != null) {
                return meta.in(bracket);
            }
            int picks = 0;
            int wins = 0;
            for (RankBracket b : RankBracket.values()) {
                picks += meta.in(b).picks();
                wins += meta.in(b).wins();
            }
            return new PickWin(picks, wins);
        }
    }
}
