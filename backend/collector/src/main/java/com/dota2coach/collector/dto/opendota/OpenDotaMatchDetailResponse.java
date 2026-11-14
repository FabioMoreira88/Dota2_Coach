package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Resposta de GET /matches/{match_id}, só com o que o Raio-X usa. Os logs e as séries por minuto só vêm em
 * partidas com replay analisado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaMatchDetailResponse(
        @JsonProperty("match_id") long matchId,
        @JsonProperty("players") List<Player> players) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Player(
            @JsonProperty("hero_id") Integer heroId,
            @JsonProperty("lh_t") List<Integer> lastHitsByMinute,
            @JsonProperty("dn_t") List<Integer> deniesByMinute,
            @JsonProperty("purchase_log") List<Purchase> purchaseLog,
            @JsonProperty("deaths_log") List<Death> deathsLog) {}

    /** {@code time} em segundos de jogo (negativo antes do início); {@code key} sem o prefixo "item_". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Purchase(@JsonProperty("time") Integer time, @JsonProperty("key") String key) {}

    /** {@code time} em segundos de jogo; {@code time_dead} em segundos. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Death(@JsonProperty("time") Integer time, @JsonProperty("time_dead") Integer timeDead) {}
}
