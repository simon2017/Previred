package cl.previred.personas.validation;

import cl.previred.personas.util.RutUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementacion de {@link Rut}: delega el chequeo de formato/digito verificador en {@link RutUtils}. */
public class RutValidator implements ConstraintValidator<Rut, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // La obligatoriedad la valida @NotBlank; aqui solo se valida el formato/DV.
            return true;
        }
        return RutUtils.esValido(value);
    }
}
