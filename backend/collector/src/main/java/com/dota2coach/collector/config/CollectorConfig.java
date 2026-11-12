package com.dota2coach.collector.config;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.client.steam.SteamOpenIdClient;
import com.dota2coach.collector.service.PlayerDataService;
import com.dota2coach.collector.service.ProfileSnapshotService;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.collector.support.Sleeper;
import com.dota2coach.core.model.PlayerSummary;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Beans do módulo de coleta. */
@Configuration
@EnableConfigurationProperties({OpenDotaProperties.class, SteamProperties.class})
public class CollectorConfig {

    private static final Duration STEAM_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public OpenDotaClient openDotaClient(RestClient.Builder builder, OpenDotaProperties properties) {
        RestClient restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()))
                .build();
        return new OpenDotaClient(
                restClient,
                properties,
                new RequestRateLimiter(properties.requestsPerMinute()),
                Sleeper.SYSTEM);
    }

    @Bean
    public SteamOpenIdClient steamOpenIdClient(RestClient.Builder builder, SteamProperties properties) {
        RestClient restClient = builder
                .requestFactory(requestFactory(STEAM_TIMEOUT, STEAM_TIMEOUT))
                .build();
        return new SteamOpenIdClient(restClient, properties.openIdEndpoint());
    }

    /**
     * Cliente HTTP do JDK forçado em HTTP/1.1: a negociação HTTP/2 do JDK com alguns servidores e proxies
     * pode travar até o timeout, e essas APIs não ganham nada com HTTP/2.
     */
    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        return ClientHttpRequestFactoryBuilder.jdk()
                .withHttpClientCustomizer(client -> client.version(HttpClient.Version.HTTP_1_1))
                .build(ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(connectTimeout)
                        .withReadTimeout(readTimeout));
    }

    @Bean
    public PlayerDataService playerDataService(OpenDotaClient client, OpenDotaProperties properties) {
        return new PlayerDataService(client, Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl())
                .maximumSize(10_000)
                .<Long, PlayerSummary>build());
    }

    @Bean
    public ProfileSnapshotService profileSnapshotService(
            OpenDotaClient client, PlayerDataService playerDataService, Clock clock) {
        return new ProfileSnapshotService(client, playerDataService, clock);
    }
}
