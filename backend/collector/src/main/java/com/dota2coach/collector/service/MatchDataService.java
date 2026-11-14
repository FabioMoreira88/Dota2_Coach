package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.dto.opendota.OpenDotaAbilityConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroConstant;
import com.dota2coach.collector.mapper.OpenDotaMatchMapper;
import com.dota2coach.collector.mapper.OpenDotaReferenceMapper;
import com.dota2coach.core.enums.DamageType;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.HeroDurations;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.HeroItemTimings;
import com.dota2coach.core.model.HeroMatchups;
import com.dota2coach.core.model.Item;
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
        return match.map(m -> {
            Map<Integer, Item> items = OpenDotaReferenceMapper.toItems(openDotaClient.getItemConstants());
            Map<String, OpenDotaAbilityConstant> abilities = abilities();
            return new MatchSnapshot(
                    m,
                    OpenDotaReferenceMapper.toHeroes(heroes),
                    items,
                    OpenDotaReferenceMapper.toDamageTypes(abilities),
                    heroDurations(m),
                    matchups(m, accountId),
                    itemPopularity(m),
                    itemTimings(m, accountId),
                    OpenDotaReferenceMapper.toAbilityNames(abilities));
        });
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

    /**
     * Habilidades do jogo, para o tipo de dano e o nome de exibição. Se a OpenDota falhar, o perfil de dano sai só
     * com ataque básico e "outro", e as habilidades aparecem pelo nome interno.
     */
    private Map<String, OpenDotaAbilityConstant> abilities() {
        try {
            return openDotaClient.getAbilityConstants();
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

    /**
     * Itens mais comprados com cada herói da partida. Só com replay analisado, porque sem ele não há compras para
     * comparar. Um herói que falha fica de fora.
     */
    private Map<Integer, HeroItemPopularity> itemPopularity(AnalyzedMatch match) {
        Map<Integer, HeroItemPopularity> popularity = new HashMap<>();
        if (!match.replayParsed()) {
            return popularity;
        }
        for (MatchPlayer player : match.players()) {
            try {
                popularity.put(player.heroId(), OpenDotaReferenceMapper.toItemPopularity(
                        player.heroId(), openDotaClient.getItemPopularity(player.heroId())));
            } catch (ExternalServiceUnavailableException e) {
                log.warn("OpenDota: sem itens populares do herói {}: {}", player.heroId(), e.getMessage());
            }
        }
        return popularity;
    }

    /**
     * Tempos de item do herói do jogador e dos cinco adversários, só com replay analisado. Um herói que falha
     * fica de fora.
     */
    private Map<Integer, HeroItemTimings> itemTimings(AnalyzedMatch match, long accountId) {
        Map<Integer, HeroItemTimings> timings = new HashMap<>();
        MatchPlayer me = match.player(accountId).orElse(null);
        if (!match.replayParsed() || me == null) {
            return timings;
        }
        for (MatchPlayer player : match.players()) {
            if (player.side() == me.side() && !player.equals(me)) {
                continue;
            }
            try {
                timings.put(player.heroId(), OpenDotaReferenceMapper.toItemTimings(
                        player.heroId(), openDotaClient.getItemTimings(player.heroId())));
            } catch (ExternalServiceUnavailableException e) {
                log.warn("OpenDota: sem tempos de item do herói {}: {}", player.heroId(), e.getMessage());
            }
        }
        return timings;
    }

    /** Pede à OpenDota a análise do replay. Os dados aparecem minutos depois. */
    public void requestReplayParsing(long matchId) {
        openDotaClient.requestParse(matchId);
    }
}
