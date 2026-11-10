package com.dota2coach.core.exception;

/** Um serviço externo (OpenDota, Steam...) não respondeu depois das tentativas. */
public class ExternalServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExternalServiceUnavailableException(String service, Throwable cause) {
        super(service + " indisponível", cause);
    }
}
