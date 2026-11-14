package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroConstant;
import com.dota2coach.collector.mapper.OpenDotaMatchMapper;
import com.dota2coach.collector.mapper.OpenDotaReferenceMapper;
import com.dota2coach.core.enums.DamageType;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.HeroDurations;
import com.dota2coach.core.model.HeroMatchups;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Busca uma partida inteira na OpenDota para a análise de partida escolhida, com o tipo de dano das habilidades
 * o desempenho por duração dos dez heróis e os matchups do herói do jogador (cerca de 15 chamadas).
 */
public class MatchDataService {

    private static final Logger log = LoggerFactory.getLogger(MatchDataService.class);

    private final OpenDotaClient openDotaClient;

    public MatchDataService(OpenDotaClient openDotaClient) {
        this.openDotaClient = openDotaClient;
    }

    /**
     * Partida com os dez jogadores, mais heróis e itens do jogo.
     *
     * @param accountId jogador analisado: os matchups são do herói dele
     * @return vazio se a OpenDota não conhece a partida ou ela ainda não tem resultado
     */
    public Optional<MatchSnapshot> collect(long matchId, long accountId) {
        Collection<OpenDotaHeroConstant> heroes = openDotaClient.getHeroConstants().values();
        Map<String, Integer> heroIdByName = heroes.stream()
                .filter(h -> h.name() != null)
                .collect(Collectors.toMap(OpenDotaHeroConstant::name, OpenDotaHeroConstant::id, (a, b) -> a));
        Optional<AnalyzedMatch> match =
                OpenDotaMatchMapper.toAnalyzedMatch(openDotaClient.getMatch(matchId), heroIdByName);
        return match.map(m -> new MatchSnapshot(
                m,
                OpenDotaReferenceMapper.toHeroes(heroes),
                OpenDotaReferenceMapper.toItems(openDotaClient.getItemConstants()),
                damageTypes(),
                heroDurations(m),
                matchups(m, accountId)));
    }

    /** Matchups do herói do jogador. Se a conta não está na partida ou a OpenDota falha, sai vazio. */
    private HeroMatchups matchups(AnalyzedMatch match, long accountId) {
        Integer heroId = match.player(accountId).map(MatchPlayer::heroId).orElse(null);
        if (heroId == null) {
            return HeroMatchups.empty(0);
        }
        try {
            return OpenDotaReferenceMapper.toHeroMatchups(heroId, openDotaClient.getHeroMatchups(heroId));
        } catch (ExternalServiceUnavailableException e) {
            log.warn("OpenDota: sem matchups do herói {}: {}", heroId, e.getMessage());
            return HeroMatchups.empty(heroId);
        }
    }

    /** Tipo de dano das habilidades. Se a OpenDota falhar, o perfil de dano sai só com ataque básico e "outro". */
    private Map<String, DamageType> damageTypes() {
        try {
            return OpenDotaReferenceMapper.toDamageTypes(openDotaClient.getAbilityConstants());
        } catch (ExternalServiceUnavailableException e) {
            log.warn("OpenDota: não foi possível buscar as habilidades; o perfil de dano fica incompleto: {}",
                    e.getMessage());
            return Map.of();
        }
    }

    /** Desempenho por duração dos heróis da partida. Um herói que falha fica de fora. */
    private Map<Integer, HeroDurations> heroDurations(AnalyzedMatch match) {
        Map<Integer, HeroDurations> durations = new HashMap<>();
        for (MatchPlayer player : match.players()) {
            try {
                durations.put(player.heroId(), OpenDotaReferenceMapper.toHeroDurations(
                        player.heroId(), openDotaClient.getHeroDurations(player.heroId())));
            } catch (ExternalServiceUnavailableException e) {
                log.warn("OpenDota: sem desempenho por duração do herói {}: {}", player.heroId(), e.getMessage());
            }
        }
        return durations;
    }

    /** Pede à OpenDota a análise do replay. Os dados aparecem minutos depois. */
    public void requestReplayParsing(long matchId) {
        openDotaClient.requestParse(matchId);
    }
}
