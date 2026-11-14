package com.dota2coach.core.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quando um herói costuma terminar cada item em partidas públicas recentes, por faixa de tempo.
 *
 * @param byItem faixas de tempo por nome interno do item (ex.: "maelstrom"), da mais cedo para a mais tarde
 */
public record HeroItemTimings(int heroId, Map<String, List<Bin>> byItem) {

    public HeroItemTimings {
        byItem = byItem.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                e -> e.getValue().stream().sorted(Comparator.comparingInt(Bin::upToSeconds)).toList()));
    }

    /**
     * Uma faixa de tempo.
     *
     * @param upToSeconds fim da faixa; ela começa no fim da faixa anterior
     */
    public record Bin(int upToSeconds, int games, int wins) {}
}
