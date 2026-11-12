package com.dota2coach.core.model;

/** Item do jogo. {@code key} é o nome interno (ex.: "blink"); {@code cost} pode ser 0 para itens sem preço. */
public record Item(int id, String key, String name, int cost) {}
