package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.HashMap;
import java.util.Map;

/**
 * Item de GET /heroStats. A OpenDota envia as escolhas e vitórias por faixa de medalha em chaves
 * numeradas ({@code "1_pick"}, {@code "1_win"} ... {@code "8_pick"}, {@code "8_win"}).
 *
 * @param bracketPicks escolhas por faixa (1 = Arauto ... 8 = Imortal)
 * @param bracketWins vitórias por faixa
 */
public record OpenDotaHeroStatsResponse(
        int id,
        Map<Integer, Integer> bracketPicks,
        Map<Integer, Integer> bracketWins,
        int proPick,
        int proWin) {

    static final int BRACKETS = 8;

    public OpenDotaHeroStatsResponse {
        bracketPicks = Map.copyOf(bracketPicks);
        bracketWins = Map.copyOf(bracketWins);
    }

    @JsonCreator
    static OpenDotaHeroStatsResponse fromJson(Map<String, Object> json) {
        Map<Integer, Integer> picks = new HashMap<>();
        Map<Integer, Integer> wins = new HashMap<>();
        for (int bracket = 1; bracket <= BRACKETS; bracket++) {
            picks.put(bracket, intValue(json.get(bracket + "_pick")));
            wins.put(bracket, intValue(json.get(bracket + "_win")));
        }
        return new OpenDotaHeroStatsResponse(
                intValue(json.get("id")), picks, wins, intValue(json.get("pro_pick")), intValue(json.get("pro_win")));
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        return 0;
    }
}
