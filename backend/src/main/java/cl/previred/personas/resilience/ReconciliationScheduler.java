package cl.previred.personas.resilience;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.entity.sqlserver.Persona;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.mapper.PersonaMapper;
import cl.previred.personas.repository.sqlserver.PersonaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reintenta periodicamente sincronizar hacia SQL Server los registros que quedaron
 * pendientes en el outbox de contingencia. Cada registro se procesa de forma aislada:
 * el fallo de uno no bloquea el resto del lote.
 */
@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final OutboxService outboxService;
    private final PersonaRepository personaRepository;
    private final PersonaMapper mapper;
    private final int maxIntentos;

    public ReconciliationScheduler(OutboxService outboxService, PersonaRepository personaRepository,
                                    PersonaMapper mapper,
                                    @Value("${app.resilience.reconciliation.max-intentos:3}") int maxIntentos) {
        this.outboxService = outboxService;
        this.personaRepository = personaRepository;
        this.mapper = mapper;
        this.maxIntentos = maxIntentos;
    }

    /** Se ejecuta periodicamente y reintenta sincronizar cada registro pendiente del outbox. */
    @Scheduled(fixedDelayString = "${app.resilience.reconciliation.fixed-delay-ms:15000}")
    public void reconciliarPendientes() {
        List<PersonaOutbox> pendientes = outboxService.obtenerPendientes();
        if (pendientes.isEmpty()) {
            return;
        }
        log.info("Reconciliando {} registro(s) pendiente(s) de sincronizacion", pendientes.size());
        for (PersonaOutbox registro : pendientes) {
            sincronizarUno(registro);
        }
    }

    /** Intenta persistir un registro en SQL Server; si falla, contabiliza el intento o lo marca FAILED. */
    private void sincronizarUno(PersonaOutbox registro) {
        try {
            PersonaRequest request = mapper.fromOutboxPayload(registro.getPayload());
            Persona persona = mapper.toEntity(request);
            // personaRepository es un proxy de Spring Data JPA independiente: cada
            // llamada a save() ya administra su propia transaccion contra SQL Server.
            personaRepository.save(persona);
            outboxService.eliminarSincronizado(registro.getRut());
            log.info("Registro {} sincronizado con exito", registro.getRut());
        } catch (Exception ex) {
            int intentos = registro.getIntentos() + 1;
            if (intentos >= maxIntentos) {
                outboxService.marcarFallidoDefinitivo(registro.getRut(), ex.getMessage());
                log.error("Registro {} supero el maximo de reintentos ({}) y quedo en estado FAILED: {}",
                        registro.getRut(), maxIntentos, ex.getMessage());
            } else {
                outboxService.registrarFallo(registro.getRut(), ex.getMessage());
                log.warn("No fue posible sincronizar el registro {} (intento {}): {}",
                        registro.getRut(), intentos, ex.getMessage());
            }
        }
    }
}
