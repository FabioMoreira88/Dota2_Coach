package com.dota2coach.core.model;

/**
 * Item do jogo.
 *
 * @param key nome interno (ex.: "blink")
 * @param cost preço em ouro; 0 para itens sem preço
 * @param assembled se é montado a partir de outros itens (BKB, Power Treads). Itens básicos como Ogre Axe ou
 *     Hyperstone são {@code false}: na maioria das partidas viram parte de outro item.
 */
public record Item(int id, String key, String name, int cost, boolean assembled) {}
