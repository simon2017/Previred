package cl.previred.personas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Direccion recibida al crear o actualizar una persona. */
public record DireccionRequest(

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 150, message = "La calle no puede superar los 150 caracteres")
        String calle,

        @NotBlank(message = "La comuna es obligatoria")
        @Size(max = 100, message = "La comuna no puede superar los 100 caracteres")
        String comuna,

        @NotBlank(message = "La region es obligatoria")
        @Size(max = 100, message = "La region no puede superar los 100 caracteres")
        String region
) {
}
