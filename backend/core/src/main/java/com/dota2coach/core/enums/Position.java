package com.dota2coach.core.enums;

/** Posição que o jogador diz jogar (1 a 5). Informada por ele, nunca deduzida das partidas. */
public enum Position {
    CARRY(1, "Carry (posição 1)"),
    MID(2, "Meio (posição 2)"),
    OFFLANE(3, "Offlane (posição 3)"),
    SOFT_SUPPORT(4, "Suporte (posição 4)"),
    HARD_SUPPORT(5, "Suporte (posição 5)");

    private final int number;
    private final String label;

    Position(int number, String label) {
        this.number = number;
        this.label = label;
    }

    public int number() {
        return number;
    }

    public String label() {
        return label;
    }

    public boolean core() {
        return number <= 3;
    }
}
