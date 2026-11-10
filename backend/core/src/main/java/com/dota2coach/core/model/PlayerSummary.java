package com.dota2coach.core.model;

import java.util.List;

/** Resumo público de um jogador: perfil, vitórias e derrotas e heróis mais jogados. */
public record PlayerSummary(
        SteamId steamId,
        String personaName,
        String avatarUrl,
        Integer rankTier,
        int wins,
        int losses,
        List<HeroStats> topHeroes) {

    public PlayerSummary {
        topHeroes = List.copyOf(topHeroes);
    }

    public int totalMatches() {
        return wins + losses;
    }

    public double winRate() {
        return totalMatches() == 0 ? 0.0 : (double) wins / totalMatches();
    }

    /** Sem partidas visíveis: dados públicos desligados ou conta ainda não indexada. */
    public boolean hasPublicMatchData() {
        return totalMatches() > 0;
    }
}
