package cl.previred.personas.util;

/**
 * Validacion y normalizacion de RUT chileno mediante el algoritmo de digito
 * verificador modulo 11.
 */
public final class RutUtils {

    private RutUtils() {
    }

    /**
     * Acepta RUT con o sin puntos/guion (ej. "12.345.678-9" o "123456789") y valida
     * su digito verificador.
     */
    public static boolean esValido(String rut) {
        if (rut == null) {
            return false;
        }
        String limpio = limpiar(rut);
        if (limpio.length() < 2) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (cuerpo.isEmpty() || !cuerpo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return calcularDv(cuerpo) == dv;
    }

    /** Retorna el RUT en formato canonico: cuerpo-DV (ej. "12345678-9"). Requiere un RUT valido. */
    public static String normalizar(String rut) {
        String limpio = limpiar(rut);
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        return cuerpo + "-" + dv;
    }

    /** Calcula el digito verificador (modulo 11) para un cuerpo de RUT sin puntos ni guion. */
    public static char calcularDv(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = (factor == 7) ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return Character.forDigit(resto, 10);
    }

    private static String limpiar(String rut) {
        return rut.replace(".", "").replace("-", "").trim().toUpperCase();
    }
}
