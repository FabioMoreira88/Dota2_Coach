package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerResponse;
import com.dota2coach.collector.mapper.OpenDotaPlayerMapper;
import com.dota2coach.core.exception.PlayerNotFoundException;
import com.dota2coach.core.model.PlayerSummary;
import com.github.benmanes.caffeine.cache.Cache;

/** Busca dados públicos do jogador na OpenDota, com cache em memória. */
public class PlayerDataService {

    static final int TOP_HEROES = 10;

    private final OpenDotaClient openDotaClient;
    private final Cache<Long, PlayerSummary> summaryCache;

    public PlayerDataService(OpenDotaClient openDotaClient, Cache<Long, PlayerSummary> summaryCache) {
        this.openDotaClient = openDotaClient;
        this.summaryCache = summaryCache;
    }

    /**
     * Resumo do jogador pelo account id de 32 bits.
     *
     * @throws PlayerNotFoundException se a OpenDota não conhece a conta
     */
    public PlayerSummary getSummary(long accountId) {
        return summaryCache.get(accountId, this::load);
    }

    private PlayerSummary load(long accountId) {
        OpenDotaPlayerResponse player = openDotaClient.getPlayer(accountId);
        if (player == null || player.profile() == null) {
            throw new PlayerNotFoundException(accountId);
        }
        return OpenDotaPlayerMapper.toPlayerSummary(
                accountId,
                player,
                openDotaClient.getWinLoss(accountId),
                openDotaClient.getHeroes(accountId),
                TOP_HEROES);
    }
}
