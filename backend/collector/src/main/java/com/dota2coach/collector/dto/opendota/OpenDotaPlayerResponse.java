package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de GET /players/{account_id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaPlayerResponse(
        @JsonProperty("profile") Profile profile,
        @JsonProperty("rank_tier") Integer rankTier) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            @JsonProperty("account_id") Long accountId,
            @JsonProperty("personaname") String personaName,
            @JsonProperty("avatarfull") String avatarFull) {}
}
