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
        PlayerSummary cached = summaryCache.getIfPresent(accountId);
        if (cached != null) {
            return cached;
        }
        Loaded loaded = load(accountId);
        if (loaded.complete()) {
            summaryCache.put(accountId, loaded.summary());
        }
        return loaded.summary();
    }

    /** Resumo e se todos os endpoints responderam; um resumo incompleto não vai para o cache. */
    private record Loaded(PlayerSummary summary, boolean complete) {}

    private Loaded load(long accountId) {
        OpenDotaPlayerResponse player = openDotaClient.getPlayer(accountId);
        if (player == null || player.profile() == null) {
            throw new PlayerNotFoundException(accountId);
        }
        List<OpenDotaPlayerHeroResponse> heroes = heroes(accountId);
        OpenDotaWinLossResponse winLoss = winLoss(accountId);
        boolean complete = heroes != null && winLoss != null;
        if (winLoss == null) {
            winLoss = heroes == null ? new OpenDotaWinLossResponse(0, 0) : sumOf(heroes);
        }
        PlayerSummary summary = OpenDotaPlayerMapper.toPlayerSummary(
                accountId, player, winLoss, heroes == null ? List.of() : heroes, TOP_HEROES);
        return new Loaded(summary, complete);
    }

    /**
     * Partidas por herói. Se o endpoint falhar (a OpenDota já deixou o /heroes fora do ar sozinho), devolve
     * {@code null}: o Raio-X não depende dele, porque calcula tudo a partir das partidas.
     */
    private List<OpenDotaPlayerHeroResponse> heroes(long accountId) {
        try {
            return openDotaClient.getHeroes(accountId);
        } catch (ExternalServiceUnavailableException e) {
            log.warn("OpenDota /heroes indisponível para a conta {}; seguindo sem os heróis mais jogados.", accountId);
            return null;
        }
    }

    /**
     * Vitórias e derrotas pelo /wl, ou {@code null} se esse endpoint falhar (também já ficou fora do ar sozinho
     * na OpenDota). Nesse caso a soma das partidas por herói dá o mesmo total.
     */
    private OpenDotaWinLossResponse winLoss(long accountId) {
        try {
            return openDotaClient.getWinLoss(accountId);
        } catch (ExternalServiceUnavailableException e) {
            log.warn("OpenDota /wl indisponível para a conta {}; usando a soma das partidas por herói.", accountId);
            return null;
        }
    }

    private static OpenDotaWinLossResponse sumOf(List<OpenDotaPlayerHeroResponse> heroes) {
        int games = heroes.stream().mapToInt(OpenDotaPlayerHeroResponse::games).sum();
        int wins = heroes.stream().mapToInt(OpenDotaPlayerHeroResponse::win).sum();
        return new OpenDotaWinLossResponse(wins, games - wins);
    }
}
