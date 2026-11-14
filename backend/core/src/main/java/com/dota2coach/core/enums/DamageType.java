package com.dota2coach.core.enums;

import java.util.Locale;

/** Tipo de dano de um ataque ou habilidade. */
public enum DamageType {
    PHYSICAL("Físico"),
    MAGICAL("Mágico"),
    PURE("Puro"),
    OTHER("Outro");

    private final String label;

    DamageType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Converte o texto da OpenDota ("Physical", "Magical", "Pure"); o resto vira {@link #OTHER}. */
    public static DamageType fromOpenDota(String value) {
        if (value == null) {
            return OTHER;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "physical" -> PHYSICAL;
            case "magical" -> MAGICAL;
            case "pure" -> PURE;
            default -> OTHER;
        };
    }
}
