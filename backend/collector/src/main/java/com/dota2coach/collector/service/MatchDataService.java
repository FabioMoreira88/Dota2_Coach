package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroConstant;
import com.dota2coach.collector.mapper.OpenDotaMatchMapper;
import com.dota2coach.collector.mapper.OpenDotaReferenceMapper;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Busca uma partida inteira na OpenDota para a análise de partida escolhida. */
public class MatchDataService {

    private final OpenDotaClient openDotaClient;

    public MatchDataService(OpenDotaClient openDotaClient) {
        this.openDotaClient = openDotaClient;
    }

    /**
     * Partida com os dez jogadores, mais heróis e itens do jogo.
     *
     * @return vazio se a OpenDota não conhece a partida ou ela ainda não tem resultado
     */
    public Optional<MatchSnapshot> collect(long matchId) {
        Collection<OpenDotaHeroConstant> heroes = openDotaClient.getHeroConstants().values();
        Map<String, Integer> heroIdByName = heroes.stream()
                .filter(h -> h.name() != null)
                .collect(Collectors.toMap(OpenDotaHeroConstant::name, OpenDotaHeroConstant::id, (a, b) -> a));
        Optional<AnalyzedMatch> match =
                OpenDotaMatchMapper.toAnalyzedMatch(openDotaClient.getMatch(matchId), heroIdByName);
        return match.map(m -> new MatchSnapshot(
                m,
                OpenDotaReferenceMapper.toHeroes(heroes),
                OpenDotaReferenceMapper.toItems(openDotaClient.getItemConstants())));
    }

    /** Pede à OpenDota a análise do replay. Os dados aparecem minutos depois. */
    public void requestReplayParsing(long matchId) {
        openDotaClient.requestParse(matchId);
    }
}
