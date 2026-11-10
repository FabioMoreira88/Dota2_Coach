package com.dota2coach.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs públicas da aplicação ({@code dota2coach.app.*}).
 *
 * @param baseUrl endereço público da API, usado no return_to do login Steam
 * @param frontendUrl para onde o navegador volta depois do login
 */
@ConfigurationProperties(prefix = "dota2coach.app")
public record AppProperties(String baseUrl, String frontendUrl) {

    public AppProperties {
        baseUrl = stripTrailingSlash(baseUrl == null ? "http://localhost:8080" : baseUrl);
        frontendUrl = stripTrailingSlash(frontendUrl == null ? "http://localhost:3000" : frontendUrl);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
