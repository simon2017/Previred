package cl.previred.personas.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validaciones para RUTs
 */
class RutUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"1-9", "6-K", "6-k", "11111111-1", "12345678-5", "76086428-5",
            "12.345.678-5", "76.086.428-5"})
    void aceptaRutsValidosConODistintoFormato(String rut) {
        assertThat(RutUtils.esValido(rut)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1-8", "6-1", "11111111-2", "12345678-4", "76086428-0"})
    void rechazaRutsConDigitoVerificadorIncorrecto(String rut) {
        assertThat(RutUtils.esValido(rut)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", "abcdefgh-5", "1"})
    void rechazaFormatosInvalidos(String rut) {
        assertThat(RutUtils.esValido(rut)).isFalse();
    }

    @Test
    void aceptaFormatoConcatenadoSinSeparadores() {
        // El ultimo caracter siempre se interpreta como digito verificador.
        assertThat(RutUtils.esValido("123456785")).isTrue();
    }

    @Test
    void rechazaNulo() {
        assertThat(RutUtils.esValido(null)).isFalse();
    }

    @Test
    void normalizaARutConGuionYDvEnMayuscula() {
        assertThat(RutUtils.normalizar("6-k")).isEqualTo("6-K");
        assertThat(RutUtils.normalizar("12.345.678-5")).isEqualTo("12345678-5");
    }

    @Test
    void calculaDigitoVerificadorK() {
        assertThat(RutUtils.calcularDv("6")).isEqualTo('K');
    }
}
