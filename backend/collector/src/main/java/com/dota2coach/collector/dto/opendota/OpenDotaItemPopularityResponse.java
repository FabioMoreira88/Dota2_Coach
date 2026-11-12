package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Resposta de GET /heroes/{hero_id}/itemPopularity: id do item para número de compras, por fase. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaItemPopularityResponse(
        @JsonProperty("start_game_items") Map<Integer, Integer> startGameItems,
        @JsonProperty("early_game_items") Map<Integer, Integer> earlyGameItems,
        @JsonProperty("mid_game_items") Map<Integer, Integer> midGameItems,
        @JsonProperty("late_game_items") Map<Integer, Integer> lateGameItems) {}
