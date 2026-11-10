package com.dota2coach.api.controller;

import com.dota2coach.api.dto.response.MeResponse;
import com.dota2coach.api.mapper.PlayerSummaryMapper;
import com.dota2coach.api.security.SteamPrincipal;
import com.dota2coach.collector.service.PlayerDataService;
import com.dota2coach.core.exception.PlayerNotFoundException;
import com.dota2coach.core.model.SteamId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dados do jogador logado. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final PlayerDataService playerDataService;
    private final PlayerSummaryMapper mapper;

    public MeController(PlayerDataService playerDataService, PlayerSummaryMapper mapper) {
        this.playerDataService = playerDataService;
        this.mapper = mapper;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal SteamPrincipal principal) {
        SteamId steamId = principal.steamId();
        try {
            return new MeResponse(Long.toString(steamId.steamId64()), steamId.accountId(),
                    mapper.toResponse(playerDataService.getSummary(steamId.accountId())));
        } catch (PlayerNotFoundException e) {
            return new MeResponse(Long.toString(steamId.steamId64()), steamId.accountId(), null);
        }
    }
}
