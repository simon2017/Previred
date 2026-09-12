package cl.previred.personas.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando SQL Server no esta disponible para una operacion que la ruta de
 * contingencia (outbox) no cubre en este alcance (actualizar/eliminar un registro que
 * ya estaba sincronizado). Ver README del backend, seccion de supuestos.
 */
public class ServicioNoDisponibleException extends PreviredException {

    public ServicioNoDisponibleException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
