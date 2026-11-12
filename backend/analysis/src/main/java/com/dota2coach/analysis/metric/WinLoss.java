package com.dota2coach.analysis.metric;

/** Partidas e vitórias de um recorte qualquer. */
public record WinLoss(int games, int wins) {

    public static final WinLoss EMPTY = new WinLoss(0, 0);

    public double winRate() {
        return games == 0 ? 0.0 : (double) wins / games;
    }

    public WinLoss plus(boolean won) {
        return new WinLoss(games + 1, wins + (won ? 1 : 0));
    }
}
