package com.dota2coach.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.dota2coach.api.config.SecurityConfig;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PingController.class)
@Import({PingControllerTest.FixedClock.class, SecurityConfig.class})
class PingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void pingRespondeOkComHorario() throws Exception {
        mvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.time").value("2026-11-16T12:00:00Z"));
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
