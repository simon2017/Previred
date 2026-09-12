package cl.previred.personas.exception;

import org.springframework.http.HttpStatus;

/** Se lanza al intentar crear una persona con un RUT que ya existe (en SQL Server o en el outbox). */
public class RutDuplicadoException extends PreviredException {

    public RutDuplicadoException(String rut) {
        super(HttpStatus.CONFLICT, "Ya existe una persona registrada con el rut " + rut);
    }
}
