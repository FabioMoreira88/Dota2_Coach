package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerHeroResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaWinLossResponse;
import com.dota2coach.collector.mapper.OpenDotaPlayerMapper;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.exception.PlayerNotFoundException;
import com.dota2coach.core.model.PlayerSummary;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Busca dados públicos do jogador na OpenDota, com cache em memória. */
public class PlayerDataService {

    private static final Logger log = LoggerFactory.getLogger(PlayerDataService.class);

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
        List<OpenDotaPlayerHeroResponse> heroes = openDotaClient.getHeroes(accountId);
        return OpenDotaPlayerMapper.toPlayerSummary(accountId, player, winLoss(accountId, heroes), heroes, TOP_HEROES);
    }

    /**
     * Vitórias e derrotas pelo /wl. Se esse endpoint falhar (já ficou fora do ar sozinho na OpenDota), soma as
     * partidas por herói, que dão o mesmo total.
     */
    private OpenDotaWinLossResponse winLoss(long accountId, List<OpenDotaPlayerHeroResponse> heroes) {
        try {
            return openDotaClient.getWinLoss(accountId);
        } catch (ExternalServiceUnavailableException e) {
            int games = heroes.stream().mapToInt(OpenDotaPlayerHeroResponse::games).sum();
            int wins = heroes.stream().mapToInt(OpenDotaPlayerHeroResponse::win).sum();
            log.warn("OpenDota /wl indisponível para a conta {}; usando a soma das partidas por herói ({} partidas).",
                    accountId, games);
            return new OpenDotaWinLossResponse(wins, games - wins);
        }
    }
}
