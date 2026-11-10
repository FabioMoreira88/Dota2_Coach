package com.dota2coach.core.model;

/** Desempenho do jogador com um herói. */
public record HeroStats(int heroId, int games, int wins) {

    public double winRate() {
        return games == 0 ? 0.0 : (double) wins / games;
    }
}
