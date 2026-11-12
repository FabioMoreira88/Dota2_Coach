package com.dota2coach.core.enums;

/** Rota em que o jogador passou a fase de rotas. Só é conhecida em partidas com replay analisado. */
public enum Lane {
    SAFE_LANE("Rota segura"),
    MID_LANE("Meio"),
    OFF_LANE("Rota difícil"),
    JUNGLE("Selva"),
    ROAMING("Roaming"),
    UNKNOWN("Desconhecida");

    private final String label;

    Lane(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
