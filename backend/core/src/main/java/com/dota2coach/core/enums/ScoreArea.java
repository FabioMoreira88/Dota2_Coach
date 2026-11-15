package com.dota2coach.core.enums;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Áreas que o coach avalia de 0 a 10 em cada partida revisada. */
public enum ScoreArea {
    LANE("Rota"),
    FIGHTS("Lutas"),
    ITEMS("Itens"),
    VISION("Visão"),
    SURVIVAL("Sobrevivência");

    private final String label;

    ScoreArea(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Pelo nome escrito na tabela, sem ligar para maiúsculas, acentos ou negrito. */
    public static Optional<ScoreArea> fromLabel(String text) {
        String wanted = plain(text);
        return Arrays.stream(values()).filter(a -> plain(a.label).equals(wanted)).findFirst();
    }

    private static String plain(String text) {
        String s = text == null ? "" : text.replace("*", "").strip();
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
