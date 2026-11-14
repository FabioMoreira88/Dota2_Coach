package com.dota2coach.core.model;

import java.util.Map;

/**
 * Tudo o que a análise de uma partida precisa, coletado de uma vez.
 *
 * @param heroes heróis do jogo por id
 * @param items itens do jogo por id
 */
public record MatchSnapshot(AnalyzedMatch match, Map<Integer, Hero> heroes, Map<Integer, Item> items) {

    public MatchSnapshot {
        heroes = Map.copyOf(heroes);
        items = Map.copyOf(items);
    }

    public String heroName(Integer heroId) {
        Hero hero = heroId == null ? null : heroes.get(heroId);
        return hero == null ? "Herói " + heroId : hero.name();
    }
}
