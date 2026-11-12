package com.dota2coach.core.enums;

import java.util.Optional;

/** Faixa de medalha. O {@code tier} segue a dezena do rank_tier da Valve (1 = Arauto ... 8 = Imortal). */
public enum RankBracket {
    HERALD(1, "Arauto"),
    GUARDIAN(2, "Guardião"),
    CRUSADER(3, "Cruzado"),
    ARCHON(4, "Arconte"),
    LEGEND(5, "Lenda"),
    ANCIENT(6, "Ancestral"),
    DIVINE(7, "Divino"),
    IMMORTAL(8, "Imortal");

    private final int tier;
    private final String label;

    RankBracket(int tier, String label) {
        this.tier = tier;
        this.label = label;
    }

    public int tier() {
        return tier;
    }

    public String label() {
        return label;
    }

    /**
     * Faixa a partir do rank_tier (ex.: 53 = Lenda 3).
     *
     * @return a faixa, ou vazio se o jogador não tem medalha
     */
    public static Optional<RankBracket> fromRankTier(Integer rankTier) {
        if (rankTier == null || rankTier < 10) {
            return Optional.empty();
        }
        int tier = rankTier / 10;
        for (RankBracket bracket : values()) {
            if (bracket.tier == tier) {
                return Optional.of(bracket);
            }
        }
        return Optional.empty();
    }
}
