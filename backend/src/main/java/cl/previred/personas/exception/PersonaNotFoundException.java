package cl.previred.personas.exception;

import org.springframework.http.HttpStatus;

/** Se lanza cuando no existe una persona (ni sincronizada ni en contingencia) con el RUT solicitado. */
public class PersonaNotFoundException extends PreviredException {

    public PersonaNotFoundException(String rut) {
        super(HttpStatus.NOT_FOUND, "No existe una persona con rut " + rut);
    }
}
