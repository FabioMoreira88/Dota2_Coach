package com.dota2coach.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da Steam ({@code dota2coach.steam.*}).
 *
 * @param openIdEndpoint endpoint OpenID 2.0 da Steam
 * @param apiKey chave da Steam Web API (usada em PRs futuros)
 */
@ConfigurationProperties(prefix = "dota2coach.steam")
public record SteamProperties(String openIdEndpoint, String apiKey) {

    public static final String DEFAULT_OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";

    public SteamProperties {
        if (openIdEndpoint == null || openIdEndpoint.isBlank()) {
            openIdEndpoint = DEFAULT_OPENID_ENDPOINT;
        }
    }
}
