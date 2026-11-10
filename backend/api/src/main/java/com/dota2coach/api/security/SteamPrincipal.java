package com.dota2coach.api.security;

import com.dota2coach.core.model.SteamId;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

/** Jogador autenticado pela Steam. Guardado na sessão: só o SteamID, nenhuma credencial. */
public record SteamPrincipal(long steamId64) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public SteamId steamId() {
        return new SteamId(steamId64);
    }

    @Override
    public String getName() {
        return Long.toString(steamId64);
    }
}
