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
 * @param comment se o coach (IA paga) comenta a partida depois do relatório
 * @param coachProvider qual provedor de {@code dota2coach.coach.providers} escreve o comentário
 */
@ConfigurationProperties(prefix = "dota2coach.partida")
public record PartidaProperties(
        Long matchId, Long accountId, Position position, String output, ZoneId zone, Boolean comment,
        String coachProvider) {

    public static final String DEFAULT_OUTPUT = "../../docs/fase0/partida-{matchId}.md";

    public PartidaProperties {
        if (output == null || output.isBlank()) {
            output = DEFAULT_OUTPUT;
        }
        if (comment == null) {
            comment = false;
        }
        if (coachProvider == null || coachProvider.isBlank()) {
            coachProvider = "claude";
        }
        if (zone == null) {
            zone = ZoneId.of("America/Sao_Paulo");
        }
    }

    public String resolvedOutput() {
        return output.replace("{matchId}", String.valueOf(matchId));
    }
}
