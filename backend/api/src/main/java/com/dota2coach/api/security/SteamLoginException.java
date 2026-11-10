package com.dota2coach.api.security;

import java.io.Serial;

/** A volta do login Steam não passou em alguma verificação. */
public class SteamLoginException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SteamLoginException(String reason) {
        super(reason);
    }
}
