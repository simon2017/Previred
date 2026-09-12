package cl.previred.personas.dto.request;

import cl.previred.personas.validation.NotFutureDate;
import cl.previred.personas.validation.Rut;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cuerpo de la solicitud para crear o actualizar una persona. Todos los campos son
 * obligatorios y se validan con Bean Validation antes de llegar al controller.
 */
public record PersonaRequest(

        @NotBlank(message = "El rut es obligatorio")
        @Rut
        String rut,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @NotFutureDate
        LocalDate fechaNacimiento,

        @NotNull(message = "La direccion es obligatoria")
        @Valid
        DireccionRequest direccion
) {
}
