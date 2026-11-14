package com.dota2coach.core.model;

import java.util.List;

/**
 * Item do jogo.
 *
 * @param key nome interno (ex.: "blink")
 * @param cost preço em ouro; 0 para itens sem preço
 * @param assembled se é montado a partir de outros itens (BKB, Power Treads). Itens básicos como Ogre Axe ou
 *     Hyperstone são {@code false}: na maioria das partidas viram parte de outro item.
 * @param components nomes internos dos itens que formam este (sem a receita); vazio se não informado
 */
public record Item(int id, String key, String name, int cost, boolean assembled, List<String> components) {

    public Item {
        components = components == null ? List.of() : List.copyOf(components);
    }

    public Item(int id, String key, String name, int cost, boolean assembled) {
        this(id, key, name, cost, assembled, List.of());
    }
}
