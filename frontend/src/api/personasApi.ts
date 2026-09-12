import type { ErrorResponse, PersonaRequest, PersonaResponse } from './types';

const BASE_URL = '/api/personas';

/**
 * Envuelve un {@link ErrorResponse} del backend para que los componentes puedan
 * distinguir un error de negocio (con mensaje y posibles fieldErrors) de una falla
 * de red inesperada.
 */
export class ApiError extends Error {
  response: ErrorResponse;

  constructor(response: ErrorResponse) {
    super(response.message);
    this.name = 'ApiError';
    this.response = response;
  }
}

async function leerRespuesta<T>(res: Response): Promise<T> {
  if (res.ok) {
    // 204 No Content (eliminar) no trae body.
    return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
  }
  const body = (await res.json().catch(() => null)) as ErrorResponse | null;
  if (body) {
    throw new ApiError(body);
  }
  throw new Error(`Error inesperado del servidor (HTTP ${res.status})`);
}

export async function listarPersonas(): Promise<PersonaResponse[]> {
  //GET
  const res = await fetch(BASE_URL);
  return await leerRespuesta(res);
}

export async function obtenerPersona(rut: string): Promise<PersonaResponse> {
  //GET
  const res = await fetch(`${BASE_URL}/${encodeURIComponent(rut)}`);
  return await leerRespuesta(res);
}

export async function crearPersona(data: PersonaRequest): Promise<PersonaResponse> {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(data),
  });
  return await leerRespuesta(res);
}

export async function actualizarPersona(rut: string, data: PersonaRequest): Promise<PersonaResponse> {
  const res = await fetch(`${BASE_URL}/${encodeURIComponent(rut)}`, {
    method: 'PUT',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(data),
  });
  return await leerRespuesta(res);
}

export async function eliminarPersona(rut: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/${encodeURIComponent(rut)}`,
      {method: 'DELETE'
  });
  return await leerRespuesta(res);
}
