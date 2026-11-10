package com.dota2coach.api.mapper;

import com.dota2coach.api.dto.response.PlayerSummaryResponse;
import com.dota2coach.api.dto.response.PlayerSummaryResponse.HeroStatsResponse;
import com.dota2coach.core.model.PlayerSummary;
import org.springframework.stereotype.Component;

/** Converte o resumo do domínio no DTO de resposta. */
@Component
public class PlayerSummaryMapper {

    public PlayerSummaryResponse toResponse(PlayerSummary summary) {
        return new PlayerSummaryResponse(
                Long.toString(summary.steamId().steamId64()),
                summary.steamId().accountId(),
                summary.personaName(),
                summary.avatarUrl(),
                summary.rankTier(),
                summary.wins(),
                summary.losses(),
                summary.totalMatches(),
                round(summary.winRate()),
                summary.hasPublicMatchData(),
                summary.topHeroes().stream()
                        .map(h -> new HeroStatsResponse(h.heroId(), h.games(), h.wins(), round(h.winRate())))
                        .toList());
    }

    private static double round(double ratio) {
        return Math.round(ratio * 10_000) / 10_000.0;
    }
}
