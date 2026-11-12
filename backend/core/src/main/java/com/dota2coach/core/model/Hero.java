package com.dota2coach.core.model;

import java.util.List;

/** Herói do jogo. {@code roles} são as funções oficiais (Carry, Support, Nuker...). */
public record Hero(int id, String name, String primaryAttribute, List<String> roles) {

    public Hero {
        roles = List.copyOf(roles);
    }
}
