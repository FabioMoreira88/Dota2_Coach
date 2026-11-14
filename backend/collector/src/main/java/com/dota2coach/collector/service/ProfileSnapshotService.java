package com.dota2coach.collector.service;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.mapper.OpenDotaMatchMapper;
import com.dota2coach.collector.mapper.OpenDotaReferenceMapper;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.model.HeroBenchmark;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.MatchDetail;
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
    /** Para quantos desses heróis buscar popularidade de itens e percentis (duas chamadas por herói). */
    static final int ITEM_POPULARITY_HEROES = 10;
    /** Quantas partidas recentes com replay analisado detalhar (uma chamada grande por partida). */
    static final int DETAILED_MATCHES = 20;
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
     * recentemente, mais os detalhes de até {@link #DETAILED_MATCHES} partidas recentes analisadas. Faz cerca de
     * 45 chamadas à OpenDota.
     */
    public ProfileSnapshot collect(long accountId) {
        PlayerSummary player = playerDataService.getSummary(accountId);
        List<MatchRecord> matches = OpenDotaMatchMapper.toMatchRecords(openDotaClient.getMatches(accountId));
        var heroes = OpenDotaReferenceMapper.toHeroes(openDotaClient.getHeroConstants().values());
        var items = OpenDotaReferenceMapper.toItems(openDotaClient.getItemConstants());
        var heroMeta = OpenDotaReferenceMapper.toHeroMeta(openDotaClient.getHeroStats());
        List<Integer> topHeroes = recentTopHeroes(matches);
        Map<Integer, HeroItemPopularity> itemPopularity = topHeroes.stream()
                .map(heroId -> OpenDotaReferenceMapper.toItemPopularity(
                        heroId, openDotaClient.getItemPopularity(heroId)))
                .collect(Collectors.toMap(HeroItemPopularity::heroId, Function.identity()));
        Map<Integer, HeroBenchmark> benchmarks = topHeroes.stream()
                .map(heroId -> OpenDotaReferenceMapper.toHeroBenchmark(heroId, openDotaClient.getBenchmarks(heroId)))
                .collect(Collectors.toMap(HeroBenchmark::heroId, Function.identity()));
        return new ProfileSnapshot(player, matches, heroes, items, heroMeta, itemPopularity, benchmarks,
                matchDetails(matches), Instant.now(clock));
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

    /** Detalhes das partidas recentes com replay analisado. Uma partida que falha fica de fora. */
    Map<Long, MatchDetail> matchDetails(List<MatchRecord> matches) {
        Map<Long, MatchDetail> details = new LinkedHashMap<>();
        matches.stream()
                .limit(RECENT_MATCHES)
                .filter(MatchRecord::parsed)
                .limit(DETAILED_MATCHES)
                .forEach(match -> {
                    try {
                        OpenDotaMatchMapper.toMatchDetail(openDotaClient.getMatch(match.matchId()), match.heroId())
                                .ifPresent(detail -> details.put(match.matchId(), detail));
                    } catch (ExternalServiceUnavailableException e) {
                        log.warn("Não foi possível detalhar a partida {}: {}", match.matchId(), e.getMessage());
                    }
                });
        return details;
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
