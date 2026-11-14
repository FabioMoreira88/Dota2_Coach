package com.dota2coach.api.config;

import com.dota2coach.core.enums.Position;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros da análise de uma partida escolhida ({@code dota2coach.partida.*}). O comando só roda quando
 * {@code match-id} é informado.
 *
 * @param matchId id da partida (aparece no cliente do Dota, no Dotabuff e na OpenDota)
 * @param accountId account id de 32 bits do jogador analisado (o "ID de amigo" do Dota)
 * @param position posição principal que o jogador informa (carry, mid, offlane, soft-support, hard-support)
 * @param output arquivo do relatório; {@code {matchId}} é trocado pelo id. Relativo à pasta backend/api.
 * @param zone fuso usado nas datas do relatório
 */
@ConfigurationProperties(prefix = "dota2coach.partida")
public record PartidaProperties(Long matchId, Long accountId, Position position, String output, ZoneId zone) {

    public static final String DEFAULT_OUTPUT = "../../docs/fase0/partida-{matchId}.md";

    public PartidaProperties {
        if (output == null || output.isBlank()) {
            output = DEFAULT_OUTPUT;
        }
        if (zone == null) {
            zone = ZoneId.of("America/Sao_Paulo");
        }
    }

    public String resolvedOutput() {
        return output.replace("{matchId}", String.valueOf(matchId));
    }
}
