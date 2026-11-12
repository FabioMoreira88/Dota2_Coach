package com.dota2coach.analysis.metric;

import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.RankBracket;
import java.time.Instant;
import java.util.List;

/**
 * Resultado do Raio-X do perfil: números calculados, sem texto de recomendação.
 *
 * @param recentWindow quantas partidas recentes entram nos recortes "recentes"
 * @param heroPoolSize heróis com pelo menos {@link XRayCalculator#POOL_MIN_GAMES} partidas no recorte recente
 * @param laneCoverage fração das partidas recentes com rota conhecida (replay analisado)
 */
public record ProfileXRay(
        String playerName,
        Integer rankTier,
        RankBracket bracket,
        Instant collectedAt,
        int recentWindow,
        Overview overview,
        List<HeroLine> heroes,
        int heroPoolSize,
        List<LaneLine> lanes,
        double laneCoverage,
        List<RoleLine> roles,
        Fundamentals fundamentals,
        List<HeroItems> items,
        double shareOnWinningMetaHeroes) {

    public ProfileXRay {
        heroes = List.copyOf(heroes);
        lanes = List.copyOf(lanes);
        roles = List.copyOf(roles);
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

    /** Função oficial do herói (Carry, Support...). Uma partida conta para cada função do herói. */
    public record RoleLine(String role, WinLoss result, double share) {}

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
