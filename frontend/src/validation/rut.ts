/**
 * Validacion y normalizacion de RUT chileno, replicando el algoritmo de digito
 * verificador modulo 11 de `RutUtils` en el backend.
 **/

function limpiar(rut: string): string {
  return rut.replaceAll('.', '').replaceAll('-', '').trim().toUpperCase();
}

function calcularDv(cuerpo: string): string {
  let suma = 0;
  let factor = 2;
  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += Number(cuerpo[i]) * factor;
    factor = factor === 7 ? 2 : factor + 1;
  }
  const resto = 11 - (suma % 11);
  if (resto === 11) return '0';
  if (resto === 10) return 'K';
  return String(resto);
}

/** Acepta RUT con o sin puntos/guion (ej. "12.345.678-9" o "123456789") y valida su DV. */
export function esRutValido(rut: string): boolean {
  const limpio = limpiar(rut);
  if (limpio.length < 2) return false;
  const cuerpo = limpio.slice(0, -1);
  const dv = limpio.slice(-1);
  if (cuerpo.length === 0 || !/^\d+$/.test(cuerpo)) return false;
  return calcularDv(cuerpo) === dv;
}

/** Retorna el RUT en formato canonico: cuerpo-DV (ej. "12345678-9"). Requiere un RUT valido. */
export function normalizarRut(rut: string): string {
  const limpio = limpiar(rut);
  const cuerpo = limpio.slice(0, -1);
  const dv = limpio.slice(-1);
  return `${cuerpo}-${dv}`;
}
