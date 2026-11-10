package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Resposta de GET /players/{account_id}/wl. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaWinLossResponse(int win, int lose) {}
