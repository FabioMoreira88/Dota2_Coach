package com.dota2coach.api.config;

import com.dota2coach.coachai.enums.ProviderType;
import com.dota2coach.coachai.service.ProviderPricing;
import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do comparativo de IA ({@code dota2coach.coach.*}). O comando só roda quando {@code account-id} é
 * informado.
 *
 * @param accountId conta cujo Raio-X será lido
 * @param report Raio-X de entrada; {@code {accountId}} é trocado pelo id. Relativo à pasta backend/api.
 * @param outputDir pasta onde saem o comparativo e o gabarito
 * @param zone fuso usado nas datas
 * @param providers modelos comparados, pelo nome que aparece no gabarito
 */
@ConfigurationProperties(prefix = "dota2coach.coach")
public record CoachProperties(
        Long accountId, String report, String outputDir, ZoneId zone, Map<String, Provider> providers) {

    public static final String DEFAULT_REPORT = "../../docs/fase0/raio-x-{accountId}.md";
    public static final String DEFAULT_OUTPUT_DIR = "../../docs/fase0";

    public CoachProperties {
        if (report == null || report.isBlank()) {
            report = DEFAULT_REPORT;
        }
        if (outputDir == null || outputDir.isBlank()) {
            outputDir = DEFAULT_OUTPUT_DIR;
        }
        if (zone == null) {
            zone = ZoneId.of("America/Sao_Paulo");
        }
        providers = providers == null ? Map.of() : new LinkedHashMap<>(providers);
    }

    public String resolvedReport() {
        return report.replace("{accountId}", String.valueOf(accountId));
    }

    /**
     * Um modelo do comparativo.
     *
     * @param type formato da API
     * @param baseUrl URL base (só para {@link ProviderType#OPENAI_COMPATIBLE})
     * @param apiKey chave, normalmente vinda do {@code .env}
     * @param model id do modelo no provedor
     * @param enabled se entra no comparativo
     * @param apiKeyRequired se sem chave o provedor é pulado (falso para o Ollama local)
     * @param effort esforço do Claude (low, medium, high, xhigh, max)
     * @param inputPricePerMillion dólares por milhão de tokens de entrada
     * @param outputPricePerMillion dólares por milhão de tokens de saída
     * @param readTimeout quanto esperar pela resposta
     */
    public record Provider(
            ProviderType type,
            String baseUrl,
            String apiKey,
            String model,
            Boolean enabled,
            Boolean apiKeyRequired,
            String effort,
            Double inputPricePerMillion,
            Double outputPricePerMillion,
            Duration readTimeout) {

        public Provider {
            if (type == null) {
                type = ProviderType.OPENAI_COMPATIBLE;
            }
            if (enabled == null) {
                enabled = true;
            }
            if (apiKeyRequired == null) {
                apiKeyRequired = true;
            }
            if (effort == null || effort.isBlank()) {
                effort = "high";
            }
            if (inputPricePerMillion == null) {
                inputPricePerMillion = 0.0;
            }
            if (outputPricePerMillion == null) {
                outputPricePerMillion = 0.0;
            }
            if (readTimeout == null) {
                readTimeout = Duration.ofMinutes(2);
            }
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        public ProviderPricing pricing() {
            return new ProviderPricing(inputPricePerMillion, outputPricePerMillion);
        }
    }
}
