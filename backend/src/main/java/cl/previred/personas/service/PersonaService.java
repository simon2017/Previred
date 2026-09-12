package cl.previred.personas.service;

import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.PersonaResponse;

import java.util.List;

/**
 * Casos de uso del CRUD de personas. El controller depende de esta abstraccion (no de
 * {@code PersonaServiceImpl}).
 * <p>
 * Cada operacion es resiliente a la caida de SQL Server: ver el javadoc de
 * {@code PersonaServiceImpl} para el detalle de como se decide entre la ruta normal y
 * la de contingencia.
 */
public interface PersonaService {

    /** Crea una persona nueva. Falla si ya existe una con el mismo RUT. */
    PersonaResponse crear(PersonaRequest request);

    /** Lista todas las personas, sincronizadas y pendientes, ordenadas por apellido y nombre. */
    List<PersonaResponse> listar();

    /** Busca una persona por RUT. Falla con {@code PersonaNotFoundException} si no existe. */
    PersonaResponse obtenerPorRut(String rut);

    /** Actualiza los datos de una persona existente. El RUT no puede cambiar. */
    PersonaResponse actualizar(String rut, PersonaRequest request);

    /** Elimina una persona por RUT. Falla con {@code PersonaNotFoundException} si no existe. */
    void eliminar(String rut);
}
