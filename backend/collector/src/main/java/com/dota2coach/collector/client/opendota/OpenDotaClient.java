package com.dota2coach.collector.client.opendota;

import com.dota2coach.collector.config.OpenDotaProperties;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerHeroResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaWinLossResponse;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.collector.support.Sleeper;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
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

    private <T> T get(Function<UriBuilder, java.net.URI> uri, ParameterizedTypeReference<T> type) {
        Duration backoff = properties.initialBackoff();
        for (int attempt = 1; ; attempt++) {
            try {
                rateLimiter.acquire();
                return restClient.get()
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
