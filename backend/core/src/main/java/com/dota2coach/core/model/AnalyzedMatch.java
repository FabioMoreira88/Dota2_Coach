package com.dota2coach.core.model;

import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Side;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Uma partida inteira, com os dez jogadores, para a análise de partida escolhida.
 *
 * @param replayParsed se a OpenDota já analisou o replay; sem isso não há rotas, logs nem séries por minuto
 * @param radiantGoldAdvantage vantagem de ouro do Radiant por minuto (negativa quando o Dire está na frente)
 * @param picks picks dos dois times, na ordem do draft quando a OpenDota informa
 * @param objectives torres, barracks, Roshan e first blood, em ordem de tempo (só com replay analisado)
 * @param teamfights lutas com 3 ou mais mortes, em ordem de tempo (só com replay analisado)
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
        List<DraftPick> picks,
        List<MatchPlayer> players,
        List<Objective> objectives,
        List<Teamfight> teamfights) {

    public AnalyzedMatch {
        radiantGoldAdvantage = List.copyOf(radiantGoldAdvantage);
        picks = List.copyOf(picks);
        players = List.copyOf(players);
        objectives = List.copyOf(objectives);
        teamfights = List.copyOf(teamfights);
    }

    /**
     * Pick de um herói.
     *
     * @param order posição no draft, começando em 1, ou nulo quando a OpenDota não informa a ordem
     */
    public record DraftPick(Integer order, int heroId, Side side) {}

    /**
     * Objetivo conquistado.
     *
     * @param time segundos de jogo
     * @param side time que conquistou: quem destruiu a construção, matou o Roshan ou tirou o first blood
     * @param building nome interno da construção destruída (ex.: "npc_dota_badguys_tower1_mid"), só em
     *     {@link ObjectiveKind#BUILDING}
     */
    public record Objective(int time, ObjectiveKind kind, Side side, String building) {}

    public enum ObjectiveKind {
        BUILDING,
        ROSHAN,
        FIRST_BLOOD
    }

    /**
     * Luta com 3 ou mais mortes, como a OpenDota detecta: começa 15 s antes da primeira morte e termina 15 s
     * depois da última.
     */
    public record Teamfight(int start, int end, List<FightPlayer> players) {

        public Teamfight {
            players = List.copyOf(players);
        }
    }

    /**
     * Um herói numa luta.
     *
     * @param goldDelta ouro ganho (ou perdido, se negativo) durante a luta
     * @param damage dano causado em heróis durante a luta
     */
    public record FightPlayer(int heroId, Side side, int deaths, int goldDelta, long damage, long healing,
            int buybacks) {}

    public Optional<MatchPlayer> player(long accountId) {
        return players.stream().filter(p -> p.accountId() != null && p.accountId() == accountId).findFirst();
    }
}
