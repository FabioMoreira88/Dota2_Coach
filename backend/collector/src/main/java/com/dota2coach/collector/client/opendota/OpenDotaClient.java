package com.dota2coach.collector.client.opendota;

import com.dota2coach.collector.config.OpenDotaProperties;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroStatsResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaItemConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaItemPopularityResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaMatchResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaParseRequestResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerHeroResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaWinLossResponse;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.collector.support.Sleeper;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import java.time.Duration;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Cliente HTTP da <a href="https://docs.opendota.com/">OpenDota API</a>.
 *
 * <p>Respeita o limite de chamadas por minuto e tenta de novo, com espera crescente, em caso de
 * 429, erro 5xx ou falha de rede. Não guarda cache: isso fica no service.
 */
public class OpenDotaClient {

    private static final Logger log = LoggerFactory.getLogger(OpenDotaClient.class);
    private static final String SERVICE = "OpenDota";

    private final RestClient restClient;
    private final OpenDotaProperties properties;
    private final RequestRateLimiter rateLimiter;
    private final Sleeper sleeper;

    public OpenDotaClient(
            RestClient restClient,
            OpenDotaProperties properties,
            RequestRateLimiter rateLimiter,
            Sleeper sleeper) {
        this.restClient = restClient;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.sleeper = sleeper;
    }

    public OpenDotaPlayerResponse getPlayer(long accountId) {
        return get(uri -> uri.path("/players/{id}").build(accountId),
                new ParameterizedTypeReference<OpenDotaPlayerResponse>() {});
    }

    public OpenDotaWinLossResponse getWinLoss(long accountId) {
        return get(uri -> uri.path("/players/{id}/wl").build(accountId),
                new ParameterizedTypeReference<OpenDotaWinLossResponse>() {});
    }

    public List<OpenDotaPlayerHeroResponse> getHeroes(long accountId) {
        List<OpenDotaPlayerHeroResponse> heroes = get(
                uri -> uri.path("/players/{id}/heroes").build(accountId),
                new ParameterizedTypeReference<List<OpenDotaPlayerHeroResponse>>() {});
        return heroes == null ? List.of() : heroes;
    }

    /**
     * Todas as partidas do jogador, da mais recente para a mais antiga. Por padrão a OpenDota devolve só
     * partidas "significativas" (exclui Turbo e modos de evento).
     */
    public List<OpenDotaMatchResponse> getMatches(long accountId) {
        List<OpenDotaMatchResponse> matches = get(
                uri -> uri.path("/players/{id}/matches")
                        .queryParam("project", OpenDotaMatchResponse.PROJECTED_FIELDS.toArray())
                        .build(accountId),
                new ParameterizedTypeReference<List<OpenDotaMatchResponse>>() {});
        return matches == null ? List.of() : matches;
    }

    /** Heróis do jogo, indexados pelo id em texto. */
    public Map<String, OpenDotaHeroConstant> getHeroConstants() {
        Map<String, OpenDotaHeroConstant> heroes = get(uri -> uri.path("/constants/heroes").build(),
                new ParameterizedTypeReference<Map<String, OpenDotaHeroConstant>>() {});
        return heroes == null ? Map.of() : heroes;
    }

    /** Itens do jogo, indexados pelo nome interno. */
    public Map<String, OpenDotaItemConstant> getItemConstants() {
        Map<String, OpenDotaItemConstant> items = get(uri -> uri.path("/constants/items").build(),
                new ParameterizedTypeReference<Map<String, OpenDotaItemConstant>>() {});
        return items == null ? Map.of() : items;
    }

    /** Escolhas e vitórias de cada herói em partidas públicas recentes, por faixa de medalha. */
    public List<OpenDotaHeroStatsResponse> getHeroStats() {
        List<OpenDotaHeroStatsResponse> stats = get(uri -> uri.path("/heroStats").build(),
                new ParameterizedTypeReference<List<OpenDotaHeroStatsResponse>>() {});
        return stats == null ? List.of() : stats;
    }

    public OpenDotaItemPopularityResponse getItemPopularity(int heroId) {
        return get(uri -> uri.path("/heroes/{id}/itemPopularity").build(heroId),
                new ParameterizedTypeReference<OpenDotaItemPopularityResponse>() {});
    }

    /**
     * Pede à OpenDota que baixe e analise o replay da partida. A análise é assíncrona: os dados aparecem
     * na partida minutos depois. Replays só ficam disponíveis por algumas semanas.
     */
    public OpenDotaParseRequestResponse requestParse(long matchId) {
        return call(HttpMethod.POST, uri -> uri.path("/request/{id}").build(matchId),
                new ParameterizedTypeReference<OpenDotaParseRequestResponse>() {});
    }

    private <T> T get(Function<UriBuilder, URI> uri, ParameterizedTypeReference<T> type) {
        return call(HttpMethod.GET, uri, type);
    }

    private <T> T call(HttpMethod method, Function<UriBuilder, URI> uri, ParameterizedTypeReference<T> type) {
        Duration backoff = properties.initialBackoff();
        for (int attempt = 1; ; attempt++) {
            try {
                rateLimiter.acquire();
                return restClient.method(method)
                        .uri(builder -> uri.apply(withApiKey(builder)))
                        .retrieve()
                        .body(type);
            } catch (HttpClientErrorException.TooManyRequests
                    | HttpServerErrorException
                    | ResourceAccessException e) {
                if (attempt >= properties.maxAttempts()) {
                    throw new ExternalServiceUnavailableException(SERVICE, e);
                }
                log.warn("OpenDota falhou (tentativa {} de {}): {}",
                        attempt, properties.maxAttempts(), e.getMessage());
                pause(backoff);
                backoff = backoff.multipliedBy(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExternalServiceUnavailableException(SERVICE, e);
            } catch (RestClientException e) {
                throw new ExternalServiceUnavailableException(SERVICE, e);
            }
        }
    }

    private UriBuilder withApiKey(UriBuilder builder) {
        return properties.hasApiKey() ? builder.queryParam("api_key", properties.apiKey()) : builder;
    }

    private void pause(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceUnavailableException(SERVICE, e);
        }
    }
}
