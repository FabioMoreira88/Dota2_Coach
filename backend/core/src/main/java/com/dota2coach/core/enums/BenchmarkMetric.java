package com.dota2coach.core.enums;

/** Métricas por partida que a OpenDota distribui em percentis para cada herói. */
public enum BenchmarkMetric {
    GOLD_PER_MIN("gold_per_min", "Ouro/min", true),
    XP_PER_MIN("xp_per_min", "XP/min", true),
    LAST_HITS_PER_MIN("last_hits_per_min", "Last hits/min", true),
    HERO_DAMAGE_PER_MIN("hero_damage_per_min", "Dano em heróis/min", true),
    DEATHS_PER_MIN("deaths_per_min", "Mortes/min", false);

    private final String openDotaKey;
    private final String label;
    private final boolean higherIsBetter;

    BenchmarkMetric(String openDotaKey, String label, boolean higherIsBetter) {
        this.openDotaKey = openDotaKey;
        this.label = label;
        this.higherIsBetter = higherIsBetter;
    }

    public String openDotaKey() {
        return openDotaKey;
    }

    public String label() {
        return label;
    }

    /** Falso para mortes: percentil alto quer dizer morrer mais que a maioria. */
    public boolean higherIsBetter() {
        return higherIsBetter;
    }
}
