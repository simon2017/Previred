package cl.previred.personas.dto.response;

import java.time.Instant;
import java.util.List;

/** Cuerpo de error uniforme devuelto por {@link cl.previred.personas.exception.GlobalExceptionHandler}. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<CampoError> fieldErrors
) {
    /** Detalle de un campo especifico que fallo la validacion, con su mensaje. */
    public record CampoError(String campo, String mensaje) {
    }
}
