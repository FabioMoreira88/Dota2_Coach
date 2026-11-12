package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Valor de GET /constants/items (mapa de nome interno para item). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaItemConstant(
        @JsonProperty("id") int id,
        @JsonProperty("dname") String displayName,
        @JsonProperty("cost") Integer cost,
        @JsonProperty("created") Boolean created) {}
