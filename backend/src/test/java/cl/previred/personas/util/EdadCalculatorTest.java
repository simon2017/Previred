package cl.previred.personas.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logica de validacion para calculadora de edad
 */
class EdadCalculatorTest {

    /**
     * Calcula la edad para un cumpleaños pasado
     */
    @Test
    void calculaEdadParaUnCumpleaniosYaOcurridoEsteAnio() {
        LocalDate hoy = LocalDate.now();
        LocalDate nacimiento = hoy.minusYears(30).minusDays(1);
        assertThat(EdadCalculator.calcular(nacimiento)).isEqualTo(30);
    }

    /**
     * Calcula la edad si el cumpleaños es hoy
     */
    @Test
    void calculaEdadCuandoElCumpleaniosEsHoy() {
        LocalDate nacimiento = LocalDate.now().minusYears(25);
        assertThat(EdadCalculator.calcular(nacimiento)).isEqualTo(25);
    }

    /**
     * Calcular la edad cuando el cumpleaños aun no ocurre este año
     */
    @Test
    void calculaEdadCuandoElCumpleaniosAunNoOcurreEsteAnio() {
        LocalDate nacimiento = LocalDate.now().minusYears(40).plusDays(2);
        assertThat(EdadCalculator.calcular(nacimiento)).isEqualTo(39);
    }

    /**
     * Calcular la edad para un recien nacido
     */
    @Test
    void retornaCeroParaUnRecienNacido() {
        assertThat(EdadCalculator.calcular(LocalDate.now())).isZero();
    }
}
