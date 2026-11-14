package com.dota2coach.api.config;

import com.dota2coach.core.enums.Position;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do comando Raio-X ({@code dota2coach.raio-x.*}). O comando só roda quando {@code account-id}
 * é informado.
 *
 * @param accountId account id de 32 bits (o "ID de amigo" do Dota)
 * @param output arquivo do relatório; {@code {accountId}} é trocado pelo id. Relativo à pasta backend/api.
 * @param parseLimit quantas partidas recentes sem replay analisado pedir à OpenDota; 0 desliga
 * @param zone fuso usado nas datas do relatório
 * @param diagnose se gera e salva o diagnóstico do coach logo depois do Raio-X (chama a IA paga)
 * @param coachProvider qual provedor de {@code dota2coach.coach.providers} escreve o diagnóstico
 * @param position posição principal que o jogador informa (carry, mid, offlane, soft-support, hard-support)
 */
@ConfigurationProperties(prefix = "dota2coach.raio-x")
public record RaioXProperties(Long accountId, String output, Integer parseLimit, ZoneId zone, Position position, Boolean diagnose, String coachProvider) {

    public static final String DEFAULT_OUTPUT = "../../docs/fase0/raio-x-{accountId}.md";

    public RaioXProperties {
        if (output == null || output.isBlank()) {
            output = DEFAULT_OUTPUT;
        }
        if (parseLimit == null || parseLimit < 0) {
            parseLimit = 20;
        }
        if (diagnose == null) {
            diagnose = false;
        }
        if (coachProvider == null || coachProvider.isBlank()) {
            coachProvider = "claude";
        }
        if (zone == null) {
            zone = ZoneId.of("America/Sao_Paulo");
        }
    }

    public String resolvedOutput() {
        return output.replace("{accountId}", String.valueOf(accountId));
    }
}
