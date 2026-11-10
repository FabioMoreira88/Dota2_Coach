package com.dota2coach.core.model;

/**
 * Identificador de uma conta Steam.
 *
 * <p>O SteamID64 (ex.: 76561197995105072) é o que a Steam usa no login; o account id de 32 bits
 * (ex.: 34839344) é o que a OpenDota e o Dota usam. A conversão é uma soma fixa.
 */
public record SteamId(long steamId64) {

    private static final long STEAM_ID64_BASE = 76_561_197_960_265_728L;

    public SteamId {
        if (steamId64 <= STEAM_ID64_BASE || steamId64 > STEAM_ID64_BASE + 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("SteamID64 inválido: " + steamId64);
        }
    }

    public static SteamId fromAccountId(long accountId) {
        return new SteamId(STEAM_ID64_BASE + accountId);
    }

    public long accountId() {
        return steamId64 - STEAM_ID64_BASE;
    }
}
