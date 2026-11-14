package com.dota2coach.core.model;

import java.util.List;

/**
 * Partidas públicas recentes de um herói por duração, para ver em que fase do jogo ele ganha mais.
 *
 * @param bins faixas de 5 minutos, em ordem
 */
public record HeroDurations(int heroId, List<Bin> bins) {

    public HeroDurations {
        bins = List.copyOf(bins);
    }

    /** @param fromSeconds início da faixa de duração, em segundos */
    public record Bin(int fromSeconds, int games, int wins) {}
}
