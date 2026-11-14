package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Resposta de GET /matches/{match_id}, só com o que o coach usa. Os logs, as séries por minuto, a rota e os
 * percentis só vêm em partidas com replay analisado ({@code version} preenchido).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaMatchDetailResponse(
        @JsonProperty("match_id") long matchId,
        @JsonProperty("start_time") Long startTime,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("radiant_win") Boolean radiantWin,
        @JsonProperty("game_mode") Integer gameMode,
        @JsonProperty("lobby_type") Integer lobbyType,
        @JsonProperty("version") Integer version,
        @JsonProperty("radiant_gold_adv") List<Integer> radiantGoldAdvantage,
        @JsonProperty("picks_bans") List<PickBan> picksBans,
        @JsonProperty("draft_timings") List<DraftTiming> draftTimings,
        @JsonProperty("players") List<Player> players,
        @JsonProperty("objectives") List<Objective> objectives,
        @JsonProperty("teamfights") List<Teamfight> teamfights) {

    /**
     * Jogador na partida.
     *
     * @param lane rota física: 1 embaixo, 2 meio, 3 em cima, 4 e 5 selva
     * @param laneRole 1 segura, 2 meio, 3 difícil, 4 selva
     * @param benchmarks percentil (0 a 1) do jogador entre as partidas públicas do herói, por métrica
     * @param damageInflictor dano causado em heróis por origem: nome da habilidade, do item, ou "null" para
     *     ataque básico
     * @param damageInflictorReceived dano recebido de heróis, pela mesma origem
     * @param damageTaken dano recebido por quem causou, pelo nome interno da unidade (herói, creep, torre)
     * @param goldReasons ouro por motivo, pelo código da Valve
     * @param xpReasons XP por motivo, pelo código da Valve
     * @param runes runas pegas por tipo, pelo código da Valve
     * @param stuns segundos de stun causados em heróis
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Player(
            @JsonProperty("player_slot") Integer playerSlot,
            @JsonProperty("account_id") Long accountId,
            @JsonProperty("hero_id") Integer heroId,
            @JsonProperty("isRadiant") Boolean radiant,
            @JsonProperty("lane") Integer lane,
            @JsonProperty("lane_role") Integer laneRole,
            @JsonProperty("is_roaming") Boolean roaming,
            @JsonProperty("kills") Integer kills,
            @JsonProperty("deaths") Integer deaths,
            @JsonProperty("assists") Integer assists,
            @JsonProperty("last_hits") Integer lastHits,
            @JsonProperty("denies") Integer denies,
            @JsonProperty("gold_per_min") Integer goldPerMin,
            @JsonProperty("xp_per_min") Integer xpPerMin,
            @JsonProperty("hero_damage") Integer heroDamage,
            @JsonProperty("tower_damage") Integer towerDamage,
            @JsonProperty("hero_healing") Integer heroHealing,
            @JsonProperty("net_worth") Integer netWorth,
            @JsonProperty("level") Integer level,
            @JsonProperty("lh_t") List<Integer> lastHitsByMinute,
            @JsonProperty("dn_t") List<Integer> deniesByMinute,
            @JsonProperty("gold_t") List<Integer> goldByMinute,
            @JsonProperty("xp_t") List<Integer> xpByMinute,
            @JsonProperty("purchase_log") List<Purchase> purchaseLog,
            @JsonProperty("deaths_log") List<Death> deathsLog,
            @JsonProperty("kills_log") List<Kill> killsLog,
            @JsonProperty("obs_log") List<Map<String, Object>> observerLog,
            @JsonProperty("sen_log") List<Map<String, Object>> sentryLog,
            @JsonProperty("lane_efficiency_pct") Integer laneEfficiencyPct,
            @JsonProperty("benchmarks") Map<String, Benchmark> benchmarks,
            @JsonProperty("damage_inflictor") Map<String, Long> damageInflictor,
            @JsonProperty("damage_inflictor_received") Map<String, Long> damageInflictorReceived,
            @JsonProperty("damage_taken") Map<String, Long> damageTaken,
            @JsonProperty("ability_uses") Map<String, Long> abilityUses,
            @JsonProperty("item_uses") Map<String, Long> itemUses,
            @JsonProperty("stuns") Double stuns,
            @JsonProperty("gold_reasons") Map<String, Long> goldReasons,
            @JsonProperty("xp_reasons") Map<String, Long> xpReasons,
            @JsonProperty("runes") Map<String, Long> runes,
            @JsonProperty("camps_stacked") Integer campsStacked,
            @JsonProperty("observer_kills") Integer observerKills,
            @JsonProperty("sentry_kills") Integer sentryKills) {}

    /** Pick ou ban da Valve. {@code team} 0 é Radiant e 1 é Dire; {@code order} começa em 0. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PickBan(
            @JsonProperty("is_pick") Boolean pick,
            @JsonProperty("hero_id") Integer heroId,
            @JsonProperty("team") Integer team,
            @JsonProperty("order") Integer order) {}

    /** Pick ou ban lido do replay (só em alguns modos). {@code order} começa em 1. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DraftTiming(
            @JsonProperty("order") Integer order,
            @JsonProperty("pick") Boolean pick,
            @JsonProperty("hero_id") Integer heroId,
            @JsonProperty("player_slot") Integer playerSlot) {}

    /** {@code time} em segundos de jogo (negativo antes do início); {@code key} sem o prefixo "item_". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Purchase(@JsonProperty("time") Integer time, @JsonProperty("key") String key) {}

    /**
     * {@code time} em segundos de jogo; {@code time_dead} em segundos; {@code key} é quem matou (ex.:
     * "npc_dota_hero_lina"), quando a OpenDota informa.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Death(
            @JsonProperty("time") Integer time,
            @JsonProperty("time_dead") Integer timeDead,
            @JsonProperty("key") String key) {}

    /** {@code key} é o herói morto (ex.: "npc_dota_hero_lina"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Kill(@JsonProperty("time") Integer time, @JsonProperty("key") String key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Benchmark(@JsonProperty("raw") Double raw, @JsonProperty("pct") Double pct) {}

    /**
     * Evento de objetivo. {@code type} "building_kill" traz em {@code key} a construção destruída;
     * "CHAT_MESSAGE_ROSHAN_KILL" traz em {@code team} o time (2 Radiant, 3 Dire); "CHAT_MESSAGE_FIRSTBLOOD"
     * traz em {@code player_slot} quem matou.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Objective(
            @JsonProperty("time") Integer time,
            @JsonProperty("type") String type,
            @JsonProperty("key") Object key,
            @JsonProperty("team") Integer team,
            @JsonProperty("player_slot") Integer playerSlot) {}

    /** Luta com 3 ou mais mortes. {@code players} tem os dez jogadores na ordem dos slots (Radiant primeiro). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Teamfight(
            @JsonProperty("start") Integer start,
            @JsonProperty("end") Integer end,
            @JsonProperty("deaths") Integer deaths,
            @JsonProperty("players") List<FightPlayer> players) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FightPlayer(
            @JsonProperty("deaths") Integer deaths,
            @JsonProperty("buybacks") Integer buybacks,
            @JsonProperty("damage") Long damage,
            @JsonProperty("healing") Long healing,
            @JsonProperty("gold_delta") Integer goldDelta) {}
}
