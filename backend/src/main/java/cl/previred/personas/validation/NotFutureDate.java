package cl.previred.personas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida que una fecha (ej. de nacimiento) no sea posterior al dia de hoy. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotFutureDateValidator.class)
public @interface NotFutureDate {

    String message() default "La fecha no puede ser futura";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
