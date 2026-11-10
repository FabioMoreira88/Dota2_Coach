package com.dota2coach.api.exception;

import com.dota2coach.api.dto.response.ErrorResponse;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.exception.PlayerNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte exceções em respostas de erro padronizadas, sem vazar detalhes internos. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PlayerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse playerNotFound(PlayerNotFoundException e) {
        return new ErrorResponse("PLAYER_NOT_FOUND", "Jogador não encontrado.");
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse invalidRequest(Exception e) {
        return new ErrorResponse("INVALID_REQUEST", "Parâmetro inválido.");
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse externalUnavailable(ExternalServiceUnavailableException e) {
        log.warn("Serviço externo indisponível", e);
        return new ErrorResponse("EXTERNAL_SERVICE_UNAVAILABLE",
                "Fonte de dados temporariamente indisponível. Tente de novo em alguns minutos.");
    }
}
