package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Item de GET /heroes/{id}/durations: partidas públicas recentes do herói por duração.
 *
 * @param durationBin início da faixa de duração, em segundos (faixas de 5 minutos)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaHeroDurationResponse(
        @JsonProperty("duration_bin") Integer durationBin,
        @JsonProperty("games_played") Integer gamesPlayed,
        @JsonProperty("wins") Integer wins) {}
