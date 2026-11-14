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
 * @param context draft, composição e matchups dos dois times
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
        Usual usual,
        Context context) {

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

    /**
     * Contexto da partida: os dois times, não só o jogador.
     *
     * @param picks picks dos dois times, na ordem do draft quando {@code pickOrderKnown}
     * @param yourTeamPickNumber em que pick do seu time você escolheu (1 a 5), ou nulo sem a ordem
     * @param teams seu time primeiro
     * @param damageReceivedByYou de que tipo foi o dano que você recebeu de heróis
     * @param matchups seu herói contra cada herói adversário, em partidas públicas recentes
     * @param power taxa de vitória média dos heróis de cada time em partidas públicas, por duração
     */
    public record Context(
            List<Pick> picks,
            boolean pickOrderKnown,
            Integer yourTeamPickNumber,
            List<TeamProfile> teams,
            DamageShare damageReceivedByYou,
            List<Matchup> matchups,
            List<PhasePower> power) {

        public static final Context EMPTY =
                new Context(List.of(), false, null, List.of(), DamageShare.NONE, List.of(), List.of());

        public Context {
            picks = List.copyOf(picks);
            teams = List.copyOf(teams);
            matchups = List.copyOf(matchups);
            power = List.copyOf(power);
        }
    }

    public record Pick(Integer order, String heroName, boolean yourTeam, boolean you) {}

    /**
     * @param damageDealt de que tipo foi o dano que o time causou em heróis
     * @param roles funções oficiais dos heróis do time, com quantos heróis têm cada uma
     */
    public record TeamProfile(boolean yours, boolean won, List<String> heroes, DamageShare damageDealt,
            List<RoleCount> roles) {

        public TeamProfile {
            heroes = List.copyOf(heroes);
            roles = List.copyOf(roles);
        }
    }

    public record RoleCount(String role, int heroes) {}

    /**
     * Divisão do dano por tipo, em frações de 0 a 1. Ataque básico conta como físico; "outro" junta itens e
     * habilidades sem tipo conhecido.
     */
    public record DamageShare(long total, double physical, double magical, double pure, double other) {

        public static final DamageShare NONE = new DamageShare(0, 0, 0, 0, 0);
    }

    /**
     * @param laneOpponent se o herói passou a fase de rotas na sua rota
     * @param winRate taxa de vitória do seu herói contra ele
     */
    public record Matchup(String heroName, boolean laneOpponent, int games, double winRate) {}

    /** Taxa de vitória média dos heróis de cada time numa faixa de duração, ou nulo sem amostra. */
    public record PhasePower(String phase, Double yourTeam, Double enemyTeam) {}
}
