package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.MatchAnalysis.ItemTime;
import com.dota2coach.analysis.metric.MatchAnalysis.PlayerItems;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Itens principais dos dez jogadores e os itens populares com cada herói que ficaram de fora. */
final class MatchItemsCalculator {

    /** Itens populares considerados por fase do jogo. */
    static final int POPULAR_PER_PHASE = 3;
    /** Quantos itens populares não feitos aparecem por jogador. */
    static final int MAX_NOT_BUILT = 3;
    /** Os itens de fim de jogo só contam se a partida durou pelo menos isso. */
    static final int LATE_ITEMS_MIN_SECONDS = 30 * 60;

    private MatchItemsCalculator() {}

    static List<PlayerItems> calculate(MatchSnapshot snapshot, MatchPlayer me, Map<String, Item> itemsByKey) {
        if (!snapshot.match().replayParsed()) {
            return List.of();
        }
        boolean late = snapshot.match().duration().toSeconds() >= LATE_ITEMS_MIN_SECONDS;
        return snapshot.match().players().stream()
                .sorted(Comparator.comparing((MatchPlayer p) -> p.side() != me.side())
                        .thenComparing(p -> !p.equals(me)))
                .map(p -> playerItems(snapshot, p, me, itemsByKey, late))
                .toList();
    }

    /**
     * Primeira compra de cada item principal, por nome interno, em ordem de compra. Um componente que aparece no
     * mesmo segundo do item que ele forma (Force Staff junto da Hurricane Pike) fica de fora: a OpenDota registra
     * os dois quando o item final é montado na hora.
     */
    static Map<String, Integer> firstCoreItems(MatchPlayer p, Map<String, Item> itemsByKey) {
        Map<String, Integer> first = new LinkedHashMap<>();
        p.purchases().forEach(b -> first.putIfAbsent(b.itemKey(), b.timeSeconds()));
        Map<String, Integer> core = new LinkedHashMap<>();
        first.forEach((key, time) -> {
            Item item = itemsByKey.get(key);
            if (item == null || !XRayCalculator.isCoreItem(item)) {
                return;
            }
            boolean builtInto = first.entrySet().stream().anyMatch(other -> other.getValue().equals(time)
                    && itemsByKey.containsKey(other.getKey())
                    && itemsByKey.get(other.getKey()).components().contains(key));
            if (!builtInto) {
                core.put(key, time);
            }
        });
        return core;
    }

    private static PlayerItems playerItems(MatchSnapshot snapshot, MatchPlayer p, MatchPlayer me,
            Map<String, Item> itemsByKey, boolean late) {
        Map<String, Integer> first = new LinkedHashMap<>();
        p.purchases().forEach(b -> first.putIfAbsent(b.itemKey(), b.timeSeconds()));
        List<ItemTime> items = firstCoreItems(p, itemsByKey).entrySet().stream()
                .map(e -> new ItemTime(itemsByKey.get(e.getKey()).name(), e.getValue()))
                .sorted(Comparator.comparingInt(ItemTime::timeSeconds))
                .toList();
        HeroItemPopularity popularity = snapshot.itemPopularity().get(p.heroId());
        List<String> notBuilt = popularity == null ? List.of()
                : popularNotBuilt(snapshot, popularity, first.keySet(), late);
        return new PlayerItems(snapshot.heroName(p.heroId()), p.side() == me.side(), p.equals(me), items, notBuilt,
                popularity != null);
    }

    private static List<String> popularNotBuilt(MatchSnapshot snapshot, HeroItemPopularity popularity,
            Set<String> boughtKeys, boolean late) {
        List<GamePhase> phases = new ArrayList<>(List.of(GamePhase.EARLY, GamePhase.MID));
        if (late) {
            phases.add(GamePhase.LATE);
        }
        Set<Item> popular = new LinkedHashSet<>();
        for (GamePhase phase : phases) {
            popularity.top(phase, Integer.MAX_VALUE).stream()
                    .map(id -> snapshot.items().get(id))
                    .filter(item -> item != null && XRayCalculator.isCoreItem(item))
                    .limit(POPULAR_PER_PHASE)
                    .forEach(popular::add);
        }
        return popular.stream()
                .filter(item -> !boughtKeys.contains(item.key()))
                .limit(MAX_NOT_BUILT)
                .map(Item::name)
                .collect(Collectors.toList());
    }
}
