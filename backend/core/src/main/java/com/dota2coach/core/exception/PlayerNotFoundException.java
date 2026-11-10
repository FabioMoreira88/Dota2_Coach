package com.dota2coach.core.exception;

/** O jogador não existe na fonte consultada. */
public class PlayerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlayerNotFoundException(long accountId) {
        super("Jogador não encontrado: " + accountId);
    }
}
