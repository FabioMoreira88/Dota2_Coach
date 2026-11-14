package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Item de GET /scenarios/itemTimings?hero_id=: em partidas públicas recentes, quantas vezes o herói terminou
 * {@code item} até {@code time} segundos (e não antes da faixa anterior), e quantas dessas partidas ganhou.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaItemTimingResponse(
        @JsonProperty("hero_id") Integer heroId,
        @JsonProperty("item") String item,
        @JsonProperty("time") Integer time,
        @JsonProperty("games") Integer games,
        @JsonProperty("wins") Integer wins) {}
