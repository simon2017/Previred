package cl.previred.personas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/** Implementacion de {@link NotFutureDate}: valida solo que la fecha no sea futura. */
public class NotFutureDateValidator implements ConstraintValidator<NotFutureDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            // La obligatoriedad la valida @NotNull; aqui solo se valida que no sea futura.
            return true;
        }
        return !value.isAfter(LocalDate.now());
    }
}
