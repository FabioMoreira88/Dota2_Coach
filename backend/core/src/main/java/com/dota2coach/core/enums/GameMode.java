package com.dota2coach.core.enums;

/** Modo de jogo da partida. */
public enum GameMode {
    ALL_PICK("All Pick"),
    TURBO("Turbo"),
    CAPTAINS_MODE("Captains Mode"),
    CAPTAINS_DRAFT("Captains Draft"),
    RANDOM_DRAFT("Random Draft"),
    SINGLE_DRAFT("Single Draft"),
    ALL_RANDOM("All Random"),
    ABILITY_DRAFT("Ability Draft"),
    OTHER("Outro");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
