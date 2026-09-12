package cl.previred.personas.mapper;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.entity.sqlserver.Direccion;
import cl.previred.personas.domain.entity.sqlserver.Persona;
import cl.previred.personas.domain.enums.EstadoSincronizacion;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.DireccionResponse;
import cl.previred.personas.dto.response.PersonaResponse;
import cl.previred.personas.util.EdadCalculator;
import cl.previred.personas.util.RutUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Traduce entre los DTOs de la API, la entidad {@link Persona} y el payload JSON que
 * se guarda en el outbox de contingencia. Es el unico lugar que sabe serializar/
 * deserializar ese payload, para no duplicar la logica de Jackson en el service.
 */
@Component
public class PersonaMapper {

    /**
     * Mensaje mostrado al cliente cuando la respuesta corresponde a un registro que
     * quedo en contingencia (SQL Server no disponible). Unica fuente de verdad: tambien
     * la usa {@code PersonaServiceImpl} para no duplicar el texto.
     */
    public static final String MENSAJE_CONTINGENCIA =
            "Registro resguardado localmente; se sincronizara automaticamente cuando la base de datos "
                    + "este disponible.";

    private final ObjectMapper objectMapper;

    public PersonaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** El RUT (normalizado) del propio request es la clave primaria; no requiere un id externo. */
    public Persona toEntity(PersonaRequest request) {
        Direccion direccion = new Direccion(
                request.direccion().calle(),
                request.direccion().comuna(),
                request.direccion().region());
        return new Persona(RutUtils.normalizar(request.rut()), request.nombre(), request.apellido(),
                request.fechaNacimiento(), direccion);
    }

    /** Construye la respuesta de la API a partir de una entidad ya persistida (o transitoria). */
    public PersonaResponse toResponse(Persona persona, EstadoSincronizacion estado, String mensaje) {
        Direccion direccion = persona.getDireccion();
        return new PersonaResponse(
                persona.getRut(),
                persona.getNombre(),
                persona.getApellido(),
                persona.getFechaNacimiento(),
                EdadCalculator.calcular(persona.getFechaNacimiento()),
                new DireccionResponse(direccion.getCalle(), direccion.getComuna(), direccion.getRegion()),
                estado,
                mensaje);
    }

    /** Reconstruye la respuesta de un registro que aun vive solo en el outbox de contingencia. */
    public PersonaResponse toResponseFromOutbox(PersonaOutbox outbox) {
        PersonaRequest request = fromOutboxPayload(outbox.getPayload());
        Persona transitoria = toEntity(request);
        return toResponse(transitoria, EstadoSincronizacion.PENDIENTE_SINCRONIZACION, MENSAJE_CONTINGENCIA);
    }

    /** Serializa el request a JSON para guardarlo como snapshot en el outbox. */
    public String toOutboxPayload(PersonaRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el registro para contingencia", e);
        }
    }

    /** Deserializa el snapshot JSON guardado en el outbox de vuelta a un {@link PersonaRequest}. */
    public PersonaRequest fromOutboxPayload(String payload) {
        try {
            return objectMapper.readValue(payload, PersonaRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible leer el registro en contingencia", e);
        }
    }
}
