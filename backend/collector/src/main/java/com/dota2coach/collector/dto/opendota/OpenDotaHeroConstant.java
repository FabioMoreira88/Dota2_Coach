package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Valor de GET /constants/heroes (mapa de id para herói). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaHeroConstant(
        @JsonProperty("id") int id,
        @JsonProperty("localized_name") String localizedName,
        @JsonProperty("primary_attr") String primaryAttr,
        @JsonProperty("roles") List<String> roles) {}
