package cl.previred.personas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida que un String sea un RUT chileno con formato y digito verificador correctos. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RutValidator.class)
public @interface Rut {

    String message() default "El rut ingresado no es valido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
