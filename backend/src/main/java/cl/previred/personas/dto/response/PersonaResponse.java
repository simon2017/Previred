package cl.previred.personas.dto.response;

import cl.previred.personas.domain.enums.EstadoSincronizacion;

import java.time.LocalDate;

/** El {@code rut} es el identificador del registro (ver {@code Persona}, es la PK). */
public record PersonaResponse(
        String rut,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        int edad,
        DireccionResponse direccion,
        EstadoSincronizacion estadoSincronizacion,
        String mensaje
) {
}
