package com.dota2coach.api.dto.response;

import java.time.Instant;

/** Resposta do endpoint de ping. */
public record PingResponse(String status, Instant time) {}
