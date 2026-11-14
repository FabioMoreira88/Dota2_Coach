package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Item de GET /heroes/{id}/matchups: partidas públicas recentes do herói contra {@code hero_id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaHeroMatchupResponse(
        @JsonProperty("hero_id") Integer heroId,
        @JsonProperty("games_played") Integer gamesPlayed,
        @JsonProperty("wins") Integer wins) {}
