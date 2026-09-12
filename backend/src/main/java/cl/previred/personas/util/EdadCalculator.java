package cl.previred.personas.util;

import java.time.LocalDate;
import java.time.Period;

/** Calcula la edad de una persona a partir de su fecha de nacimiento. */
public final class EdadCalculator {

    private EdadCalculator() {
    }

    /** Edad actual en anios completos. No se almacena: se calcula en cada lectura. */
    public static int calcular(LocalDate fechaNacimiento) {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
}
