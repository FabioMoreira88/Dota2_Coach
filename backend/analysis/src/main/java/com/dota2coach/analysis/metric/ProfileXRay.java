package com.dota2coach.analysis.metric;

import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.RankBracket;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resultado do Raio-X do perfil: números calculados, sem texto de recomendação.
 *
 * @param recentWindow quantas partidas recentes entram nos recortes "recentes"
 * @param heroPoolSize heróis com pelo menos {@link XRayCalculator#POOL_MIN_GAMES} partidas no recorte recente
 * @param mainPosition posição que o jogador informou, ou nulo se não informou
 * @param laneCoverage fração das partidas recentes com rota conhecida (replay analisado)
 */
public record ProfileXRay(
        String playerName,
        Integer rankTier,
        RankBracket bracket,
        Instant collectedAt,
        Position mainPosition,
        int recentWindow,
        Overview overview,
        List<HeroLine> heroes,
        int heroPoolSize,
        List<LaneLine> lanes,
        double laneCoverage,
        Fundamentals fundamentals,
        List<HeroBenchmarkLine> benchmarks,
        List<HeroItems> items,
        MatchDetails details,
        double shareOnWinningMetaHeroes) {

    public ProfileXRay {
        heroes = List.copyOf(heroes);
        lanes = List.copyOf(lanes);
        benchmarks = List.copyOf(benchmarks);
        items = List.copyOf(items);
    }

    /**
     * Visão geral.
     *
     * @param allTime todas as partidas significativas que a OpenDota conhece
     * @param recent partidas do recorte recente
     * @param last30Days partidas nos últimos 30 dias
     */
    public record Overview(
            WinLoss allTime,
            WinLoss recent,
            int rankedRecent,
            int abandonedRecent,
            int partyRecent,
            int last30Days,
            Instant firstMatch,
            Instant lastMatch) {}

    /**
     * Desempenho com um herói.
     *
     * @param bracketWinRate taxa de vitória do herói na faixa do jogador (meta), ou nulo sem dados
     * @param bracketPickRate fração das partidas da faixa em que o herói aparece, ou nulo sem dados
     */
    public record HeroLine(
            int heroId,
            String name,
            WinLoss recent,
            WinLoss allTime,
            double kda,
            double goldPerMin,
            double xpPerMin,
            Double bracketWinRate,
            Double bracketPickRate) {}

    public record LaneLine(Lane lane, WinLoss result) {}


    /**
     * Médias do jogador com um herói nas partidas recentes, comparadas com todas as partidas públicas do herói.
     *
     * @param metrics só as métricas para as quais a OpenDota tem dados
     */
    public record HeroBenchmarkLine(int heroId, String name, int games, Map<BenchmarkMetric, MetricComparison> metrics) {

        public HeroBenchmarkLine {
            metrics = Map.copyOf(metrics);
        }
    }

    /**
     * @param playerValue média do jogador
     * @param percentile fração (0 a 1) das partidas públicas do herói com valor menor ou igual; 0 e 1 indicam fora
     *     da faixa publicada (abaixo de 10% ou acima de 99%)
     */
    public record MetricComparison(double playerValue, double percentile) {}

    /**
     * Números das partidas recentes com replay analisado.
     *
     * @param analyzedMatches partidas detalhadas
     * @param lastHitsAt10 média de last hits aos 10 minutos, ou nulo sem partidas que passaram dos 10 minutos
     * @param deniesAt10 média de denies aos 10 minutos, ou nulo
     * @param deathsByPhase mortes por fase da partida
     * @param secondsDeadPerMatch tempo morto médio por partida
     * @param itemTimings minuto da primeira compra dos itens principais, por herói
     */
    public record MatchDetails(
            int analyzedMatches,
            Double lastHitsAt10,
            Double deniesAt10,
            List<HeroLaning> laningByHero,
            List<DeathPhase> deathsByPhase,
            double secondsDeadPerMatch,
            List<ItemTiming> itemTimings) {

        public static final MatchDetails EMPTY = new MatchDetails(0, null, null, List.of(), List.of(), 0, List.of());

        public MatchDetails {
            laningByHero = List.copyOf(laningByHero);
            deathsByPhase = List.copyOf(deathsByPhase);
            itemTimings = List.copyOf(itemTimings);
        }
    }

    public record HeroLaning(String heroName, int matches, double lastHitsAt10) {}

    /**
     * @param label fase, ex.: "0 a 10 min"
     * @param deathsPerMatch mortes na fase divididas pelas partidas analisadas
     * @param share fração de todas as mortes que aconteceu nessa fase
     */
    public record DeathPhase(String label, double deathsPerMatch, double share) {}

    /**
     * @param matches partidas do herói em que o item foi comprado
     * @param medianMinute mediana do minuto da primeira compra
     */
    public record ItemTiming(String heroName, String itemName, int matches, double medianMinute) {}

    /** Médias das últimas partidas comparadas com as anteriores do recorte recente. */
    public record Fundamentals(StatBlock latest, StatBlock previous) {}

    public record StatBlock(
            int matches,
            double winRate,
            double goldPerMin,
            double xpPerMin,
            double lastHitsPerMin,
            double deaths,
            double kda,
            double heroDamagePerMin,
            double durationMinutes) {

        public static final StatBlock EMPTY = new StatBlock(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Itens de um herói comparados com os mais comprados por todos (meta).
     *
     * @param adherence fração dos itens principais do jogador que estão entre os itens populares do herói
     * @param missingMeta itens populares do herói que o jogador quase nunca termina a partida com
     */
    public record HeroItems(
            int heroId,
            String heroName,
            int matchesWithItems,
            List<ItemUse> playerTop,
            List<String> metaTop,
            double adherence,
            List<String> missingMeta) {

        public HeroItems {
            playerTop = List.copyOf(playerTop);
            metaTop = List.copyOf(metaTop);
            missingMeta = List.copyOf(missingMeta);
        }
    }

    /** Item no inventário final: em quantas partidas apareceu e se está entre os populares do herói. */
    public record ItemUse(int itemId, String name, int matches, boolean meta) {}
}
