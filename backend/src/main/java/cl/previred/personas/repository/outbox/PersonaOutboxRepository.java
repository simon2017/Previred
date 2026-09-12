package cl.previred.personas.repository.outbox;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.enums.EstadoOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso al almacen de contingencia (H2). El RUT es la PK, igual que en {@code Persona}. */
public interface PersonaOutboxRepository extends JpaRepository<PersonaOutbox, String> {

    /** Usado por el scheduler de reconciliacion para obtener los registros aun no sincronizados. */
    List<PersonaOutbox> findByEstado(EstadoOutbox estado);
}
