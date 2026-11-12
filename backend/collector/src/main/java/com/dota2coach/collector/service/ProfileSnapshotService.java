package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.mapper.OpenDotaMatchMapper;
import com.dota2coach.collector.mapper.OpenDotaReferenceMapper;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Coleta na OpenDota tudo o que o Raio-X precisa e pede a análise dos replays recentes. */
public class ProfileSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ProfileSnapshotService.class);

    /** Quantas partidas recentes definem os heróis "atuais" do jogador. */
    static final int RECENT_MATCHES = 100;
    /** Para quantos desses heróis buscar a popularidade de itens (uma chamada por herói). */
    static final int ITEM_POPULARITY_HEROES = 10;
    /** A Valve mantém os replays por cerca de duas semanas; depois disso a OpenDota não consegue analisar. */
    static final Duration REPLAY_AVAILABILITY = Duration.ofDays(14);

    private final OpenDotaClient openDotaClient;
    private final PlayerDataService playerDataService;
    private final Clock clock;

    public ProfileSnapshotService(OpenDotaClient openDotaClient, PlayerDataService playerDataService, Clock clock) {
        this.openDotaClient = openDotaClient;
        this.playerDataService = playerDataService;
        this.clock = clock;
    }

    /**
     * Coleta perfil, partidas, heróis, itens, meta por faixa e itens populares dos heróis mais jogados
     * recentemente. Faz cerca de 15 chamadas à OpenDota.
     */
    public ProfileSnapshot collect(long accountId) {
        PlayerSummary player = playerDataService.getSummary(accountId);
        List<MatchRecord> matches = OpenDotaMatchMapper.toMatchRecords(openDotaClient.getMatches(accountId));
        var heroes = OpenDotaReferenceMapper.toHeroes(openDotaClient.getHeroConstants().values());
        var items = OpenDotaReferenceMapper.toItems(openDotaClient.getItemConstants());
        var heroMeta = OpenDotaReferenceMapper.toHeroMeta(openDotaClient.getHeroStats());
        Map<Integer, HeroItemPopularity> itemPopularity = recentTopHeroes(matches).stream()
                .map(heroId -> OpenDotaReferenceMapper.toItemPopularity(
                        heroId, openDotaClient.getItemPopularity(heroId)))
                .collect(Collectors.toMap(HeroItemPopularity::heroId, Function.identity()));
        return new ProfileSnapshot(
                player, matches, heroes, items, heroMeta, itemPopularity, Instant.now(clock));
    }

    /**
     * Pede a análise das partidas recentes que ainda não foram analisadas e cujo replay ainda existe.
     * Uma falha em uma partida não interrompe as demais.
     *
     * @param matches partidas do jogador, da mais recente para a mais antiga
     * @param limit máximo de pedidos
     * @return ids das partidas cuja análise foi pedida
     */
    public List<Long> requestReplayParsing(List<MatchRecord> matches, int limit) {
        Instant oldestWithReplay = Instant.now(clock).minus(REPLAY_AVAILABILITY);
        List<Long> requested = new ArrayList<>();
        for (MatchRecord match : matches) {
            if (requested.size() >= limit || match.startTime().isBefore(oldestWithReplay)) {
                break;
            }
            if (match.parsed()) {
                continue;
            }
            try {
                openDotaClient.requestParse(match.matchId());
                requested.add(match.matchId());
            } catch (ExternalServiceUnavailableException e) {
                log.warn("Não foi possível pedir a análise da partida {}: {}", match.matchId(), e.getMessage());
            }
        }
        return requested;
    }

    /** Heróis mais jogados nas partidas recentes, do mais jogado para o menos. */
    static List<Integer> recentTopHeroes(List<MatchRecord> matches) {
        Map<Integer, Long> games = matches.stream()
                .limit(RECENT_MATCHES)
                .collect(Collectors.groupingBy(MatchRecord::heroId, LinkedHashMap::new, Collectors.counting()));
        return games.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(ITEM_POPULARITY_HEROES)
                .map(Map.Entry::getKey)
                .toList();
    }
}
