package com.dota2coach.core.model;

import java.util.List;

/**
 * Detalhes de uma partida com replay analisado, do ponto de vista do jogador.
 *
 * @param lastHitsAt10 last hits aos 10 minutos, ou nulo se a partida acabou antes
 * @param deniesAt10 denies aos 10 minutos, ou nulo se a partida acabou antes
 * @param purchases compras em ordem, com o tempo de jogo em segundos
 * @param deaths mortes em ordem, com o tempo de jogo e o tempo morto em segundos
 */
public record MatchDetail(
        long matchId,
        int heroId,
        Integer lastHitsAt10,
        Integer deniesAt10,
        List<Purchase> purchases,
        List<Death> deaths) {

    public MatchDetail {
        purchases = List.copyOf(purchases);
        deaths = List.copyOf(deaths);
    }

    /** Compra de um item pelo nome interno (ex.: "black_king_bar"). */
    public record Purchase(String itemKey, int timeSeconds) {}

    public record Death(int timeSeconds, int secondsDead) {}
}
