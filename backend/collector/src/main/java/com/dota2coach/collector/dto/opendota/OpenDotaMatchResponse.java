package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Item de GET /players/{account_id}/matches com os campos pedidos em {@code project}.
 *
 * <p>Campos que dependem do replay analisado (rota, itens) chegam nulos quando a partida não foi analisada.
 * {@code version} preenchido indica que a OpenDota analisou o replay.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaMatchResponse(
        @JsonProperty("match_id") long matchId,
        @JsonProperty("player_slot") Integer playerSlot,
        @JsonProperty("radiant_win") Boolean radiantWin,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("game_mode") Integer gameMode,
        @JsonProperty("lobby_type") Integer lobbyType,
        @JsonProperty("hero_id") int heroId,
        @JsonProperty("start_time") long startTime,
        @JsonProperty("version") Integer version,
        @JsonProperty("kills") Integer kills,
        @JsonProperty("deaths") Integer deaths,
        @JsonProperty("assists") Integer assists,
        @JsonProperty("leaver_status") Integer leaverStatus,
        @JsonProperty("party_size") Integer partySize,
        @JsonProperty("gold_per_min") Integer goldPerMin,
        @JsonProperty("xp_per_min") Integer xpPerMin,
        @JsonProperty("last_hits") Integer lastHits,
        @JsonProperty("denies") Integer denies,
        @JsonProperty("hero_damage") Integer heroDamage,
        @JsonProperty("tower_damage") Integer towerDamage,
        @JsonProperty("hero_healing") Integer heroHealing,
        @JsonProperty("lane_role") Integer laneRole,
        @JsonProperty("is_roaming") Boolean isRoaming,
        @JsonProperty("item_0") Integer item0,
        @JsonProperty("item_1") Integer item1,
        @JsonProperty("item_2") Integer item2,
        @JsonProperty("item_3") Integer item3,
        @JsonProperty("item_4") Integer item4,
        @JsonProperty("item_5") Integer item5,
        @JsonProperty("item_neutral") Integer itemNeutral) {

    /** Campos pedidos à OpenDota em {@code project}. Mantenha em sincronia com os componentes acima. */
    public static final List<String> PROJECTED_FIELDS = List.of(
            "match_id", "player_slot", "radiant_win", "duration", "game_mode", "lobby_type", "hero_id",
            "start_time", "version", "kills", "deaths", "assists", "leaver_status", "party_size",
            "gold_per_min", "xp_per_min", "last_hits", "denies", "hero_damage", "tower_damage",
            "hero_healing", "lane_role", "is_roaming", "item_0", "item_1", "item_2", "item_3", "item_4",
            "item_5", "item_neutral");
}
