package com.dota2coach.core.model;

import com.dota2coach.core.enums.GameMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Uma partida inteira, com os dez jogadores, para a análise de partida escolhida.
 *
 * @param replayParsed se a OpenDota já analisou o replay; sem isso não há rotas, logs nem séries por minuto
 * @param radiantGoldAdvantage vantagem de ouro do Radiant por minuto (negativa quando o Dire está na frente)
 */
public record AnalyzedMatch(
        long matchId,
        Instant startTime,
        Duration duration,
        boolean radiantWin,
        GameMode gameMode,
        boolean ranked,
        boolean replayParsed,
        List<Integer> radiantGoldAdvantage,
        List<MatchPlayer> players) {

    public AnalyzedMatch {
        radiantGoldAdvantage = List.copyOf(radiantGoldAdvantage);
        players = List.copyOf(players);
    }

    public Optional<MatchPlayer> player(long accountId) {
        return players.stream().filter(p -> p.accountId() != null && p.accountId() == accountId).findFirst();
    }
}
