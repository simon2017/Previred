/** Tipos que reflejan los DTOs del backend (ver `backend/.../dto/*`). */

export interface Direccion {
  calle: string;
  comuna: string;
  region: string;
}

export interface PersonaRequest {
  rut: string;
  nombre: string;
  apellido: string;
  /** Formato ISO `yyyy-MM-dd`. */
  fechaNacimiento: string;
  direccion: Direccion;
}

/** Ver `EstadoSincronizacion` en el backend. */
export type EstadoSincronizacion = 'SINCRONIZADO' | 'PENDIENTE_SINCRONIZACION';

export interface PersonaResponse {
  rut: string;
  nombre: string;
  apellido: string;
  fechaNacimiento: string;
  edad: number;
  direccion: Direccion;
  estadoSincronizacion: EstadoSincronizacion;
  mensaje: string | null;
}

export interface CampoError {
  campo: string;
  mensaje: string;
}

/** Ver `ErrorResponse` en el backend: cuerpo de error uniforme para toda la API. */
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: CampoError[];
}
