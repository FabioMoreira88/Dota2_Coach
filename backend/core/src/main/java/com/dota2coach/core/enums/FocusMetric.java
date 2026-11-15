package com.dota2coach.core.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Métricas que um foco do diagnóstico pode usar como meta. A chave é o que a IA escreve no bloco de metas; o app
 * calcula o valor de cada uma no Raio-X e na partida, sem depender do texto da IA.
 */
public enum FocusMetric {
    WIN_RATE("taxa_vitoria", "Taxa de vitória", true, Unit.PERCENT, null),
    GOLD_PER_MIN("ouro_por_min", "Ouro por minuto", true, Unit.NUMBER, null),
    XP_PER_MIN("xp_por_min", "XP por minuto", true, Unit.NUMBER, null),
    LAST_HITS_PER_MIN("last_hits_por_min", "Last hits por minuto", true, Unit.NUMBER, null),
    HERO_DAMAGE_PER_MIN("dano_em_herois_por_min", "Dano em heróis por minuto", true, Unit.NUMBER, null),
    DEATHS_PER_MATCH("mortes_por_partida", "Mortes por partida", false, Unit.NUMBER, null),
    KDA("kda", "KDA", true, Unit.NUMBER, null),
    LAST_HITS_AT_10("last_hits_aos_10", "Last hits aos 10 min", true, Unit.NUMBER, null),
    DENIES_AT_10("denies_aos_10", "Denies aos 10 min", true, Unit.NUMBER, null),
    DEATHS_0_10("mortes_0_10", "Mortes de 0 a 10 min", false, Unit.NUMBER, "0 a 10 min"),
    DEATHS_10_20("mortes_10_20", "Mortes de 10 a 20 min", false, Unit.NUMBER, "10 a 20 min"),
    DEATHS_20_30("mortes_20_30", "Mortes de 20 a 30 min", false, Unit.NUMBER, "20 a 30 min"),
    DEATHS_30_PLUS("mortes_30_mais", "Mortes depois dos 30 min", false, Unit.NUMBER, "30 min ou mais"),
    SECONDS_DEAD("tempo_morto", "Tempo morto por partida (s)", false, Unit.SECONDS, null);

    /** Como o número é mostrado. */
    public enum Unit {
        PERCENT,
        NUMBER,
        SECONDS
    }

    private final String key;
    private final String label;
    private final boolean higherIsBetter;
    private final Unit unit;
    private final String deathPhase;

    FocusMetric(String key, String label, boolean higherIsBetter, Unit unit, String deathPhase) {
        this.key = key;
        this.label = label;
        this.higherIsBetter = higherIsBetter;
        this.unit = unit;
        this.deathPhase = deathPhase;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }

    public Unit unit() {
        return unit;
    }

    /** Fase das mortes (ex.: "10 a 20 min") nas métricas de mortes por fase, ou nulo. */
    public String deathPhase() {
        return deathPhase;
    }

    public static Optional<FocusMetric> fromKey(String key) {
        return Arrays.stream(values()).filter(m -> m.key.equalsIgnoreCase(key == null ? "" : key.strip())).findFirst();
    }
}
