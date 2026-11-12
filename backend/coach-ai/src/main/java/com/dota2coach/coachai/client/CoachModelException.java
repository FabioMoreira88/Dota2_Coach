package com.dota2coach.coachai.client;

/** O provedor de IA falhou, recusou o pedido ou devolveu uma resposta vazia. */
public class CoachModelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CoachModelException(String message) {
        super(message);
    }

    public CoachModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
