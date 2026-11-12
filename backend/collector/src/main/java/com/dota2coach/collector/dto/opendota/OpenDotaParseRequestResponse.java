package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de POST /request/{match_id}: o job de análise do replay criado na fila da OpenDota. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaParseRequestResponse(@JsonProperty("job") Job job) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Job(@JsonProperty("jobId") Long jobId) {}
}
