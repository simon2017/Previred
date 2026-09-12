package cl.previred.personas.exception;

import org.springframework.http.HttpStatus;

/**
 * Base para las excepciones de negocio de la app. Cada subclase fija el
 * {@link HttpStatus} con el que {@link GlobalExceptionHandler} debe responder, de modo
 * que el controller no necesita saber de codigos HTTP.
 */
public abstract class PreviredException extends RuntimeException {

    private final HttpStatus status;

    protected PreviredException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
