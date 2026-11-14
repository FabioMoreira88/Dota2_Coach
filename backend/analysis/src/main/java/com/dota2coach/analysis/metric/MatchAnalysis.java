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
 * @param timeline objetivos e lutas que decidiram a partida (vazio sem replay analisado)
 * @param playersItems itens principais dos dez jogadores, seu time primeiro e você no topo (vazio sem replay
 *     analisado)
 * @param itemTimings seus itens principais e os do adversário direto, comparados com quando os heróis costumam
 *     terminá-los (vazio sem replay analisado)
 * @param play de quem veio o dano, uso de itens e habilidades, fontes de ouro e XP, visão e mapa
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
        Context context,
        Timeline timeline,
        List<PlayerItems> playersItems,
        List<ItemTimingLine> itemTimings,
        PlayDetails play) {

    public MatchAnalysis {
        percentiles = Map.copyOf(percentiles);
        deaths = List.copyOf(deaths);
        deathsByPhase = List.copyOf(deathsByPhase);
        items = List.copyOf(items);
        teamGold = List.copyOf(teamGold);
        playersItems = List.copyOf(playersItems);
        itemTimings = List.copyOf(itemTimings);
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
     * @param directOpponent herói adversário na posição que você enfrenta diretamente (2 contra 2, 1 contra 3, 4
     *     contra 5), ou nulo sem posição informada ou sem replay analisado
     */
    public record Context(
            List<Pick> picks,
            boolean pickOrderKnown,
            Integer yourTeamPickNumber,
            List<TeamProfile> teams,
            DamageShare damageReceivedByYou,
            List<Matchup> matchups,
            List<PhasePower> power,
            String directOpponent) {

        public static final Context EMPTY =
                new Context(List.of(), false, null, List.of(), DamageShare.NONE, List.of(), List.of(), null);

        public Context {
            picks = List.copyOf(picks);
            teams = List.copyOf(teams);
            matchups = List.copyOf(matchups);
            power = List.copyOf(power);
        }
    }

    /**
     * @param position posição de 1 a 5: a informada, para você, ou a estimada pela rota e pelo farm, para os
     *     outros; nulo quando não se sabe
     * @param positionBasis de onde veio a posição estimada (ex.: "rota segura, menor patrimônio do time"), ou
     *     nulo para a sua posição e quando não há estimativa
     * @param unusualPick se o herói foi jogado numa posição que não combina com as funções oficiais dele
     */
    public record Pick(Integer order, String heroName, boolean yourTeam, boolean you, Integer position,
            String positionBasis, boolean unusualPick) {}

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
     * @param directOpponent se é o adversário direto da sua posição
     * @param winRate taxa de vitória do seu herói contra ele
     */
    public record Matchup(String heroName, boolean laneOpponent, boolean directOpponent, int games,
            double winRate) {}

    /** Taxa de vitória média dos heróis de cada time numa faixa de duração, ou nulo sem amostra. */
    public record PhasePower(String phase, Double yourTeam, Double enemyTeam) {}

    /**
     * Como a partida foi decidida.
     *
     * @param objectives torres, barracks, Ancient, Roshans e first blood, em ordem de tempo
     * @param fightsTotal lutas com 3 ou mais mortes que a OpenDota detectou
     * @param fightsWithYou lutas em que você causou dano, curou ou morreu
     * @param fightsWonByGold lutas em que seu time ganhou mais ouro que o adversário
     * @param biggestFights as lutas que mais mexeram no ouro, em ordem de tempo
     * @param replayMoments trechos para rever no replay, em ordem de tempo
     */
    public record Timeline(List<ObjectiveLine> objectives, int fightsTotal, int fightsWithYou, int fightsWonByGold,
            List<FightLine> biggestFights, List<ReplayMoment> replayMoments) {

        public static final Timeline EMPTY = new Timeline(List.of(), 0, 0, 0, List.of(), List.of());

        public Timeline {
            objectives = List.copyOf(objectives);
            biggestFights = List.copyOf(biggestFights);
            replayMoments = List.copyOf(replayMoments);
        }
    }

    /** Trecho da partida para rever no replay, em segundos de jogo, e por quê. */
    public record ReplayMoment(int fromSeconds, int toSeconds, String reason) {}

    /**
     * @param event o que aconteceu, para vir depois de quem fez (ex.: "derrubou a torre T1 do meio")
     * @param yourTeam se foi o seu time que conquistou
     */
    public record ObjectiveLine(int timeSeconds, String event, boolean yourTeam) {}

    /**
     * Uma luta.
     *
     * @param goldSwing ouro que seu time ganhou na luta menos o que o adversário ganhou (negativo se perdeu)
     * @param yourTeamDead heróis do seu time que morreram, com repetição se morreram mais de uma vez
     * @param youTookPart se você causou dano, curou ou morreu na luta
     * @param buybacks buybacks do seu time na luta
     */
    public record FightLine(int startSeconds, int endSeconds, int goldSwing, List<String> yourTeamDead,
            List<String> enemiesDead, boolean youTookPart, boolean youDied, int buybacks) {

        public FightLine {
            yourTeamDead = List.copyOf(yourTeamDead);
            enemiesDead = List.copyOf(enemiesDead);
        }
    }

    /**
     * Itens de um jogador na partida.
     *
     * @param items primeira compra de cada item principal, em ordem de tempo
     * @param popularNotBuilt itens principais mais comprados com o herói em partidas públicas que o jogador não
     *     fez; vazio se a OpenDota não informou
     * @param popularKnown se a OpenDota informou os itens populares do herói
     */
    public record PlayerItems(String heroName, boolean yourTeam, boolean you, List<ItemTime> items,
            List<String> popularNotBuilt, boolean popularKnown) {

        public PlayerItems {
            items = List.copyOf(items);
            popularNotBuilt = List.copyOf(popularNotBuilt);
        }
    }

    public record ItemTime(String itemName, int timeSeconds) {}

    /**
     * Quando um item principal saiu, comparado com as partidas públicas recentes do herói.
     *
     * @param usualRange faixa de tempo em que o herói mais costuma terminar o item (a faixa da mediana), ex.:
     *     "12 a 15 min"
     * @param comparison "antes do normal", "no normal" ou "depois do normal"
     * @param winRate taxa de vitória do herói quando termina o item na mesma faixa de tempo, ou nulo com pouca
     *     amostra ou depois da última faixa
     */
    public record ItemTimingLine(String heroName, boolean you, String itemName, int timeSeconds, String usualRange,
            String comparison, Double winRate) {}

    /**
     * Detalhes do jogo do jogador que só o replay analisado tem.
     *
     * @param damageFromHeroes dano recebido de cada herói adversário, do maior para o menor, com a fração do dano
     *     total recebido
     * @param damageFromOthersShare fração do dano total recebido que veio de creeps, torres e Roshan
     * @param goldLostOnDeaths ouro perdido ao morrer
     * @param goldOnBuybacks ouro gasto em buyback
     * @param goldSources de onde veio o ouro ganho, do maior para o menor
     * @param xpSources de onde veio o XP, do maior para o menor
     * @param itemUses itens ativados, do mais usado para o menos usado
     * @param abilityUses habilidades usadas, da mais usada para a menos usada
     * @param stunSeconds tempo total de stun causado em heróis
     * @param runes runas pegas por tipo
     */
    public record PlayDetails(
            List<DamageFrom> damageFromHeroes,
            double damageFromOthersShare,
            long goldLostOnDeaths,
            long goldOnBuybacks,
            List<Share> goldSources,
            List<Share> xpSources,
            List<Use> itemUses,
            List<Use> abilityUses,
            double stunSeconds,
            int observerKills,
            int sentryKills,
            int campsStacked,
            List<Use> runes) {

        public static final PlayDetails EMPTY = new PlayDetails(List.of(), 0, 0, 0, List.of(), List.of(), List.of(),
                List.of(), 0, 0, 0, 0, List.of());

        public PlayDetails {
            damageFromHeroes = List.copyOf(damageFromHeroes);
            goldSources = List.copyOf(goldSources);
            xpSources = List.copyOf(xpSources);
            itemUses = List.copyOf(itemUses);
            abilityUses = List.copyOf(abilityUses);
            runes = List.copyOf(runes);
        }
    }

    public record DamageFrom(String heroName, long damage, double share) {}

    /** Parte de um total, com a fração de 0 a 1. */
    public record Share(String label, long value, double share) {}

    public record Use(String name, long count) {}
}
