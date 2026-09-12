package cl.previred.personas.exception;

import org.springframework.http.HttpStatus;

/**
 * El RUT es la clave primaria de una persona (ver {@code Persona}), asi que se trata
 * como inmutable: un PUT no puede "mover" un registro a otro RUT. Se lanza cuando el
 * rut del cuerpo de la solicitud no coincide con el rut de la URL.
 */
public class RutInmutableException extends PreviredException {

    public RutInmutableException() {
        super(HttpStatus.BAD_REQUEST,
                "El rut no puede modificarse: es el identificador del registro. "
                        + "Elimina y crea un nuevo registro si necesitas cambiarlo.");
    }
}
