package com.dota2coach.collector.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do cliente OpenDota ({@code dota2coach.opendota.*}).
 *
 * @param baseUrl URL base da API
 * @param apiKey chave paga opcional; vazia usa o plano gratuito
 * @param requestsPerMinute limite de chamadas por minuto respeitado pelo cliente
 * @param maxAttempts tentativas por chamada em caso de 429, 5xx ou falha de rede
 * @param initialBackoff espera antes da segunda tentativa; dobra a cada nova tentativa
 * @param connectTimeout tempo máximo para abrir a conexão
 * @param readTimeout tempo máximo esperando a resposta (a OpenDota gratuita às vezes passa de 10 s)
 * @param cacheTtl validade do cache de dados de jogador
 */
@ConfigurationProperties(prefix = "dota2coach.opendota")
public record OpenDotaProperties(
        String baseUrl,
        String apiKey,
        int requestsPerMinute,
        int maxAttempts,
        Duration initialBackoff,
        Duration connectTimeout,
        Duration readTimeout,
        Duration cacheTtl) {

    public OpenDotaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.opendota.com/api";
        }
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 50;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 3;
        }
        if (initialBackoff == null) {
            initialBackoff = Duration.ofSeconds(1);
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(30);
        }
        if (cacheTtl == null) {
            cacheTtl = Duration.ofMinutes(15);
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
