package com.dota2coach.analysis.metric;

import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Números de uma partida escolhida pelo jogador, comparados com os adversários de rota e com o padrão dele (o
 * último Raio-X salvo). Sem texto de recomendação.
 *
 * @param position posição que o jogador informou, ou nulo
 * @param replayParsed sem replay analisado, rotas, mortes, itens, visão e percentis ficam vazios
 * @param killParticipation (abates + assistências) / abates do time, ou nulo se o time não matou ninguém
 * @param percentiles percentil (0 a 1) do jogador entre as partidas públicas do herói, por métrica
 * @param usual padrão do jogador segundo o último Raio-X, ou nulo se ainda não há Raio-X salvo
 */
public record MatchAnalysis(
        long matchId,
        Instant startTime,
        Duration duration,
        boolean won,
        GameMode gameMode,
        boolean ranked,
        boolean replayParsed,
        String heroName,
        Position position,
        Lane lane,
        Line line,
        Double killParticipation,
        Map<BenchmarkMetric, Double> percentiles,
        Double lastHitsAt10Percentile,
        Laning laning,
        List<DeathLine> deaths,
        List<DeathPhaseLine> deathsByPhase,
        List<ItemLine> items,
        int observersPlaced,
        int sentriesPlaced,
        List<TeamGold> teamGold,
        Usual usual) {

    public MatchAnalysis {
        percentiles = Map.copyOf(percentiles);
        deaths = List.copyOf(deaths);
        deathsByPhase = List.copyOf(deathsByPhase);
        items = List.copyOf(items);
        teamGold = List.copyOf(teamGold);
    }

    /** Números finais do jogador na partida. */
    public record Line(
            int kills,
            int deaths,
            int assists,
            int lastHits,
            int denies,
            int goldPerMin,
            int xpPerMin,
            int heroDamage,
            int towerDamage,
            int heroHealing,
            int netWorth,
            int level) {}

    /**
     * Fase de rotas, aos 10 minutos. Valores nulos quando a partida acabou antes ou o replay não foi analisado.
     *
     * @param opponents adversários que passaram a fase de rotas na mesma rota
     * @param laneEfficiencyPct eficiência calculada pela OpenDota (0 a 100)
     */
    public record Laning(
            Integer lastHitsAt10,
            Integer deniesAt10,
            Integer goldAt10,
            Integer xpAt10,
            Integer laneEfficiencyPct,
            List<LaneOpponent> opponents) {

        public static final Laning EMPTY = new Laning(null, null, null, null, null, List.of());

        public Laning {
            opponents = List.copyOf(opponents);
        }
    }

    public record LaneOpponent(String heroName, Integer lastHitsAt10, Integer deniesAt10, Integer goldAt10,
            Integer xpAt10) {}

    /**
     * @param killer herói que matou, ou nulo se foi torre, creep, Roshan ou desconhecido
     */
    public record DeathLine(int timeSeconds, String phase, String killer, int secondsDead) {}

    /**
     * @param usualPerMatch média de mortes nessa fase segundo o último Raio-X, ou nulo
     */
    public record DeathPhaseLine(String phase, int deaths, Double usualPerMatch) {}

    /**
     * Primeira compra de um item principal.
     *
     * @param usualMinute mediana do jogador com o herói segundo o último Raio-X, ou nulo
     */
    public record ItemLine(String itemName, int timeSeconds, Double usualMinute) {}

    /** Vantagem de ouro do time do jogador (negativa quando está atrás) num minuto. */
    public record TeamGold(int minute, int advantage) {}

    /**
     * Padrão do jogador segundo o último Raio-X.
     *
     * @param heroGames partidas recentes com o herói no Raio-X (0 se não jogou o herói no recorte)
     * @param lastHitsAt10 média de last hits aos 10 minutos com o herói, ou no geral se não há dados do herói
     * @param lastHitsAt10ForHero se {@code lastHitsAt10} é a média com o herói
     * @param deniesAt10 média geral de denies aos 10 minutos
     */
    public record Usual(
            Instant collectedAt,
            int heroGames,
            Double heroGoldPerMin,
            Double heroXpPerMin,
            Double heroKda,
            Double lastHitsAt10,
            boolean lastHitsAt10ForHero,
            Double deniesAt10,
            double secondsDeadPerMatch) {}
}
