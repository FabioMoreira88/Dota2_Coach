package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Item de GET /players/{account_id}/heroes. A OpenDota envia hero_id como texto ou número. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaPlayerHeroResponse(
        @JsonProperty("hero_id") int heroId,
        @JsonProperty("games") int games,
        @JsonProperty("win") int win,
        @JsonProperty("last_played") long lastPlayed) {}
