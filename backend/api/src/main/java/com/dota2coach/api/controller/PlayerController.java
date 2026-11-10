package com.dota2coach.api.controller;

import com.dota2coach.api.dto.response.PlayerSummaryResponse;
import com.dota2coach.api.mapper.PlayerSummaryMapper;
import com.dota2coach.collector.service.PlayerDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dados públicos de um jogador.
 *
 * <p>Provisório para testes da fase 0: quando o login Steam entrar, o uso normal passa a ser
 * {@code /me}, e este endpoint fica restrito.
 */
@Validated
@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerDataService playerDataService;
    private final PlayerSummaryMapper mapper;

    public PlayerController(PlayerDataService playerDataService, PlayerSummaryMapper mapper) {
        this.playerDataService = playerDataService;
        this.mapper = mapper;
    }

    @GetMapping("/{accountId}/summary")
    public PlayerSummaryResponse summary(@PathVariable @Positive @Max(0xFFFF_FFFFL) long accountId) {
        return mapper.toResponse(playerDataService.getSummary(accountId));
    }
}
