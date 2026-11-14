package com.dota2coach.core.model;

import com.dota2coach.core.enums.DamageType;
import java.util.Map;

/**
 * Tudo o que a análise de uma partida precisa, coletado de uma vez.
 *
 * @param heroes heróis do jogo por id
 * @param items itens do jogo por id
 * @param damageTypes tipo de dano de cada habilidade, pelo nome interno
 * @param heroDurations desempenho por duração dos heróis da partida, por id
 * @param playerMatchups desempenho do herói do jogador analisado contra cada herói, em partidas públicas
 * @param itemPopularity itens mais comprados com cada herói da partida, por id do herói (vazio sem replay
 *     analisado)
 * @param itemTimings quando o herói costuma terminar cada item, para o herói do jogador e os adversários (vazio
 *     sem replay analisado)
 * @param abilityNames nome de exibição das habilidades pelo nome interno
 */
public record MatchSnapshot(
        AnalyzedMatch match,
        Map<Integer, Hero> heroes,
        Map<Integer, Item> items,
        Map<String, DamageType> damageTypes,
        Map<Integer, HeroDurations> heroDurations,
        HeroMatchups playerMatchups,
        Map<Integer, HeroItemPopularity> itemPopularity,
        Map<Integer, HeroItemTimings> itemTimings,
        Map<String, String> abilityNames) {

    public MatchSnapshot {
        heroes = Map.copyOf(heroes);
        items = Map.copyOf(items);
        damageTypes = Map.copyOf(damageTypes);
        heroDurations = Map.copyOf(heroDurations);
        itemPopularity = Map.copyOf(itemPopularity);
        itemTimings = Map.copyOf(itemTimings);
        abilityNames = Map.copyOf(abilityNames);
    }

    public String heroName(Integer heroId) {
        Hero hero = heroId == null ? null : heroes.get(heroId);
        return hero == null ? "Herói " + heroId : hero.name();
    }
}
