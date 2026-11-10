package com.dota2coach.api.controller;

import com.dota2coach.api.dto.response.PingResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint simples para conferir que a API está no ar. */
@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final Clock clock;

    public PingController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", Instant.now(clock));
    }
}
