package com.dota2coach.collector.config;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.client.steam.SteamOpenIdClient;
import com.dota2coach.collector.service.PlayerDataService;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.collector.support.Sleeper;
import com.dota2coach.core.model.PlayerSummary;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(
                        ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(properties.timeout())
                                .withReadTimeout(properties.timeout())))
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
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(
                        ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(STEAM_TIMEOUT)
                                .withReadTimeout(STEAM_TIMEOUT)))
                .build();
        return new SteamOpenIdClient(restClient, properties.openIdEndpoint());
    }

    @Bean
    public PlayerDataService playerDataService(OpenDotaClient client, OpenDotaProperties properties) {
        return new PlayerDataService(client, Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl())
                .maximumSize(10_000)
                .<Long, PlayerSummary>build());
    }
}
