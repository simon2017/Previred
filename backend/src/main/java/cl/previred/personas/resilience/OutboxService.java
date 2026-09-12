package cl.previred.personas.resilience;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.enums.EstadoOutbox;
import cl.previred.personas.domain.enums.TipoOperacion;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.mapper.PersonaMapper;
import cl.previred.personas.repository.outbox.PersonaOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Encapsula todo el acceso al almacen de contingencia (H2 local). Cada metodo corre en
 * su propia transaccion contra el {@code outboxTransactionManager}, totalmente
 * independiente de cualquier transaccion (fallida) contra SQL Server. Los registros se
 * identifican por RUT normalizado (misma PK que {@code Persona} en SQL Server).
 */
@Service
public class OutboxService {

    private final PersonaOutboxRepository outboxRepository;
    private final PersonaMapper mapper;

    public OutboxService(PersonaOutboxRepository outboxRepository, PersonaMapper mapper) {
        this.outboxRepository = outboxRepository;
        this.mapper = mapper;
    }

    /**
     * Guarda un nuevo registro pendiente. Usa {@code REQUIRES_NEW} para que el outbox
     * quede confirmado aunque quien llama este en medio de manejar otra excepcion.
     */
    @Transactional(value = "outboxTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void encolarCreacion(String rut, PersonaRequest request) {
        outboxRepository.save(new PersonaOutbox(rut, TipoOperacion.CREATE, mapper.toOutboxPayload(request)));
    }

    /** Registros aun no sincronizados con SQL Server, candidatos a reconciliacion. */
    @Transactional(value = "outboxTransactionManager", readOnly = true)
    public List<PersonaOutbox> obtenerPendientes() {
        return outboxRepository.findByEstado(EstadoOutbox.PENDING);
    }

    /** Busca un registro pendiente por RUT; vacio si no existe o si ya fue sincronizado/fallo definitivamente. */
    @Transactional(value = "outboxTransactionManager", readOnly = true)
    public Optional<PersonaOutbox> buscarPendientePorRut(String rut) {
        return outboxRepository.findById(rut).filter(o -> o.getEstado() == EstadoOutbox.PENDING);
    }

    /** Reemplaza el payload de un registro aun pendiente (ej. tras un PUT sobre el mismo RUT). */
    @Transactional("outboxTransactionManager")
    public void actualizarPendiente(String rut, PersonaRequest request) {
        outboxRepository.findById(rut).ifPresent(o -> o.actualizarPayload(mapper.toOutboxPayload(request)));
    }

    /** Descarta un registro pendiente (ej. tras un DELETE sobre un registro que aun no llegaba a SQL Server). */
    @Transactional("outboxTransactionManager")
    public void eliminarPendiente(String rut) {
        outboxRepository.deleteById(rut);
    }

    /**
     * Elimina un registro que se sincronizo exitosamente con SQL Server: una vez que el
     * dato ya vive en el almacen definitivo, el outbox no necesita retenerlo (a
     * diferencia de un registro {@code FAILED}, que se conserva para intervencion
     * manual, ver {@link #marcarFallidoDefinitivo}).
     */
    @Transactional("outboxTransactionManager")
    public void eliminarSincronizado(String rut) {
        outboxRepository.deleteById(rut);
    }

    /** Registra un intento de sincronizacion fallido, sin agotar aun los reintentos. */
    @Transactional("outboxTransactionManager")
    public void registrarFallo(String rut, String error) {
        outboxRepository.findById(rut).ifPresent(o -> o.registrarFallo(error));
    }

    /** Marca un registro como definitivamente fallido tras agotar los reintentos. */
    @Transactional("outboxTransactionManager")
    public void marcarFallidoDefinitivo(String rut, String error) {
        outboxRepository.findById(rut).ifPresent(o -> o.marcarFallidoDefinitivo(error));
    }
}
