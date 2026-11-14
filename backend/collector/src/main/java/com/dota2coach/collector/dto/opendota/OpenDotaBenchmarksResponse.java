package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Resposta de GET /benchmarks?hero_id=...: para cada métrica, o valor em cada percentil. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaBenchmarksResponse(
        @JsonProperty("hero_id") Integer heroId,
        @JsonProperty("result") Map<String, List<Point>> result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Point(
            @JsonProperty("percentile") double percentile,
            @JsonProperty("value") Double value) {}
}
