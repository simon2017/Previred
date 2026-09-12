package cl.previred.personas.exception;

import cl.previred.personas.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Traduce cualquier excepcion no capturada por el controller a un {@link ErrorResponse}
 * uniforme, centralizando en un solo lugar el mapeo entre tipo de error y codigo HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Excepciones de negocio propias de la app: cada una ya trae su HttpStatus (ver {@link PreviredException}). */
    @ExceptionHandler(PreviredException.class)
    public ResponseEntity<ErrorResponse> handlePrevired(PreviredException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    /** Falla de {@code @Valid} sobre el cuerpo de la solicitud (ej. un {@code PersonaRequest} invalido). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.CampoError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Uno o mas campos no son validos", request, errores);
    }

    /** Falla de validacion sobre un path variable o request param (ej. {@code @Rut} en la URL). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                    HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.CampoError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Uno o mas parametros no son validos", request, errores);
    }

    /** El cuerpo de la solicitud no es un JSON valido o no calza con el DTO esperado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato valido", request,
                List.of());
    }

    /** Red de seguridad: cualquier excepcion no anticipada se registra y responde como 500 generico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado procesando {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado", request, List.of());
    }

    /** Ensambla el {@link ErrorResponse} comun a todos los handlers de esta clase. */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                 List<ErrorResponse.CampoError> errores) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
