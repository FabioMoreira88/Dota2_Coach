package com.dota2coach.core.model;

import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import java.util.List;
import java.util.Map;

/**
 * Um dos dez jogadores de uma {@link AnalyzedMatch}. As séries por minuto, os logs, a rota e os percentis ficam
 * vazios ou nulos quando o replay não foi analisado.
 *
 * @param accountId nulo quando o jogador esconde os dados
 * @param physicalLane rota física da fase de rotas: 1 embaixo, 2 meio, 3 em cima, 4 e 5 selva; nulo se desconhecida
 * @param laneEfficiencyPct eficiência na fase de rotas calculada pela OpenDota (0 a 100), ou nulo
 * @param benchmarkPercentiles percentil (0 a 1) entre as partidas públicas do herói, por métrica
 * @param lastHitsAt10Percentile percentil dos last hits aos 10 minutos, ou nulo
 * @param damageDealtBySource dano causado em heróis por origem: habilidade ou item pelo nome interno, ou
 *     {@link #BASIC_ATTACK}
 * @param damageReceivedBySource dano recebido de heróis, pela mesma origem
 */
public record MatchPlayer(
        int slot,
        Long accountId,
        int heroId,
        Side side,
        Lane lane,
        Integer physicalLane,
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
        int level,
        List<Integer> lastHitsByMinute,
        List<Integer> deniesByMinute,
        List<Integer> goldByMinute,
        List<Integer> xpByMinute,
        List<MatchDetail.Purchase> purchases,
        List<Death> deathLog,
        List<Kill> killLog,
        int observersPlaced,
        int sentriesPlaced,
        Integer laneEfficiencyPct,
        Map<BenchmarkMetric, Double> benchmarkPercentiles,
        Double lastHitsAt10Percentile,
        Map<String, Long> damageDealtBySource,
        Map<String, Long> damageReceivedBySource) {

    /** Origem do dano de ataque básico (a OpenDota usa "null"). */
    public static final String BASIC_ATTACK = "null";

    public MatchPlayer {
        lastHitsByMinute = List.copyOf(lastHitsByMinute);
        deniesByMinute = List.copyOf(deniesByMinute);
        goldByMinute = List.copyOf(goldByMinute);
        xpByMinute = List.copyOf(xpByMinute);
        purchases = List.copyOf(purchases);
        deathLog = List.copyOf(deathLog);
        killLog = List.copyOf(killLog);
        benchmarkPercentiles = Map.copyOf(benchmarkPercentiles);
        damageDealtBySource = Map.copyOf(damageDealtBySource);
        damageReceivedBySource = Map.copyOf(damageReceivedBySource);
    }

    /** Valor da série no minuto, ou nulo se a partida acabou antes ou a série não existe. */
    public static Integer atMinute(List<Integer> byMinute, int minute) {
        return byMinute == null || byMinute.size() <= minute ? null : byMinute.get(minute);
    }

    /**
     * Morte do jogador.
     *
     * @param killerHeroId herói que matou, ou nulo se foi torre, creep, Roshan ou desconhecido
     */
    public record Death(int timeSeconds, int secondsDead, Integer killerHeroId) {}

    /** Abate feito pelo jogador. */
    public record Kill(int timeSeconds, Integer victimHeroId) {}
}
