package com.dota2coach.core.model;

import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Uma partida do jogador, do ponto de vista dele.
 *
 * @param items itens finais nos seis espaços do inventário (sem espaços vazios); vazio se a OpenDota não tem o dado
 * @param neutralItem item neutro final, ou 0
 * @param lane rota na fase de rotas; {@link Lane#UNKNOWN} quando o replay não foi analisado
 * @param parsed se a OpenDota analisou o replay (dados de rota, itens por tempo etc.)
 * @param abandoned se o jogador abandonou a partida
 */
public record MatchRecord(
        long matchId,
        Instant startTime,
        Duration duration,
        int heroId,
        Side side,
        boolean won,
        GameMode gameMode,
        boolean ranked,
        int partySize,
        boolean abandoned,
        int kills,
        int deaths,
        int assists,
        int goldPerMin,
        int xpPerMin,
        int lastHits,
        int denies,
        int heroDamage,
        int towerDamage,
        int heroHealing,
        Lane lane,
        List<Integer> items,
        int neutralItem,
        boolean parsed) {

    public MatchRecord {
        items = List.copyOf(items);
    }

    /** KDA no formato usual: (abates + assistências) / max(1, mortes). */
    public double kda() {
        return (double) (kills + assists) / Math.max(1, deaths);
    }

    public double durationMinutes() {
        return duration.toSeconds() / 60.0;
    }
}
