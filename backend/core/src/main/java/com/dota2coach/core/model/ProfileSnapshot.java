package com.dota2coach.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tudo o que o Raio-X precisa sobre um jogador, coletado de uma vez.
 *
 * @param matches partidas do jogador, da mais recente para a mais antiga
 * @param heroes heróis do jogo por id
 * @param items itens do jogo por id
 * @param heroMeta escolhas e vitórias públicas por herói
 * @param itemPopularity itens populares dos heróis que o jogador mais joga
 */
public record ProfileSnapshot(
        PlayerSummary player,
        List<MatchRecord> matches,
        Map<Integer, Hero> heroes,
        Map<Integer, Item> items,
        Map<Integer, HeroMeta> heroMeta,
        Map<Integer, HeroItemPopularity> itemPopularity,
        Instant collectedAt) {

    public ProfileSnapshot {
        matches = List.copyOf(matches);
        heroes = Map.copyOf(heroes);
        items = Map.copyOf(items);
        heroMeta = Map.copyOf(heroMeta);
        itemPopularity = Map.copyOf(itemPopularity);
    }

    public String heroName(int heroId) {
        Hero hero = heroes.get(heroId);
        return hero == null ? "Herói " + heroId : hero.name();
    }

    public String itemName(int itemId) {
        Item item = items.get(itemId);
        return item == null ? "Item " + itemId : item.name();
    }
}
