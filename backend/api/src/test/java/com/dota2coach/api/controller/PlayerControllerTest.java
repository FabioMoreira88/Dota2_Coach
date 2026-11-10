package com.dota2coach.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dota2coach.api.mapper.PlayerSummaryMapper;
import com.dota2coach.collector.service.PlayerDataService;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.exception.PlayerNotFoundException;
import com.dota2coach.core.model.HeroStats;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.SteamId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.dota2coach.api.config.SecurityConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
@Import({PlayerSummaryMapper.class, SecurityConfig.class})
class PlayerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PlayerDataService playerDataService;

    @Test
    void devolveResumoDoJogador() throws Exception {
        when(playerDataService.getSummary(34839344L)).thenReturn(new PlayerSummary(
                SteamId.fromAccountId(34839344L), "Jogador Teste", "https://a/b.jpg", 53, 3, 1,
                List.of(new HeroStats(14, 4, 3))));

        mvc.perform(get("/api/v1/players/34839344/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId64").value("76561197995105072"))
                .andExpect(jsonPath("$.totalMatches").value(4))
                .andExpect(jsonPath("$.winRate").value(0.75))
                .andExpect(jsonPath("$.publicMatchData").value(true))
                .andExpect(jsonPath("$.topHeroes[0].heroId").value(14));
    }

    @Test
    void jogadorInexistenteDa404() throws Exception {
        when(playerDataService.getSummary(1L)).thenThrow(new PlayerNotFoundException(1L));

        mvc.perform(get("/api/v1/players/1/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
    }

    @Test
    void openDotaForaDoArDa503() throws Exception {
        when(playerDataService.getSummary(2L))
                .thenThrow(new ExternalServiceUnavailableException("OpenDota", new RuntimeException()));

        mvc.perform(get("/api/v1/players/2/summary"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
    }

    @Test
    void idInvalidoDa400() throws Exception {
        mvc.perform(get("/api/v1/players/-5/summary")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/players/abc/summary")).andExpect(status().isBadRequest());
    }
}
