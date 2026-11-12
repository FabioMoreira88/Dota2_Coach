package com.dota2coach.core.model;

import com.dota2coach.core.enums.GamePhase;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Itens mais comprados com um herói em partidas recentes, por fase do jogo (id do item para contagem). */
public record HeroItemPopularity(int heroId, Map<GamePhase, Map<Integer, Integer>> byPhase) {

    public HeroItemPopularity {
        byPhase = byPhase.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
    }

    /** Os {@code limit} itens mais comprados na fase, do mais comum para o menos comum. */
    public List<Integer> top(GamePhase phase, int limit) {
        return byPhase.getOrDefault(phase, Map.of()).entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
