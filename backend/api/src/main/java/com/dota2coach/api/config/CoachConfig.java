package com.dota2coach.api.config;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.dota2coach.coachai.client.AnthropicCoachClient;
import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.client.OpenAiCompatibleCoachClient;
import com.dota2coach.coachai.service.CoachComparisonService;
import com.dota2coach.coachai.service.ComparisonReportRenderer;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.function.BiFunction;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** Beans do módulo coach-ai, que não depende de Spring. */
@Configuration
@EnableConfigurationProperties(CoachProperties.class)
public class CoachConfig {

    /** Fábrica de clientes de IA: recebe o nome e a configuração do provedor. */
    public interface CoachClientFactory extends BiFunction<String, CoachProperties.Provider, CoachModelClient> {}

    @Bean
    public CoachComparisonService coachComparisonService() {
        return new CoachComparisonService(new SecureRandom());
    }

    @Bean
    public ComparisonReportRenderer comparisonReportRenderer(CoachProperties properties) {
        return new ComparisonReportRenderer(properties.zone());
    }

    @Bean
    public CoachClientFactory coachClientFactory(Clock clock) {
        return (name, provider) -> switch (provider.type()) {
            case ANTHROPIC -> new AnthropicCoachClient(
                    name,
                    AnthropicOkHttpClient.builder().apiKey(provider.apiKey()).build(),
                    provider.model(),
                    provider.effort(),
                    clock);
            case OPENAI_COMPATIBLE -> new OpenAiCompatibleCoachClient(
                    name, openAiRestClient(provider), provider.model(), clock);
        };
    }

    private static RestClient openAiRestClient(CoachProperties.Provider provider) {
        if (provider.baseUrl() == null || provider.baseUrl().isBlank()) {
            throw new IllegalStateException("Falta base-url para o provedor de IA " + provider.model());
        }
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(provider.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .withHttpClientCustomizer(client -> client.version(HttpClient.Version.HTTP_1_1))
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(Duration.ofSeconds(10))
                                .withReadTimeout(provider.readTimeout())));
        if (provider.hasApiKey()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.apiKey());
        }
        return builder.build();
    }
}
