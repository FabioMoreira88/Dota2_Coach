package com.dota2coach.api.dto.response;

import java.util.List;

/** Resumo público do jogador devolvido pela API. */
public record PlayerSummaryResponse(
        String steamId64,
        long accountId,
        String personaName,
        String avatarUrl,
        Integer rankTier,
        int wins,
        int losses,
        int totalMatches,
        double winRate,
        boolean publicMatchData,
        List<HeroStatsResponse> topHeroes) {

    /** Desempenho com um herói. */
    public record HeroStatsResponse(int heroId, int games, int wins, double winRate) {}
}
