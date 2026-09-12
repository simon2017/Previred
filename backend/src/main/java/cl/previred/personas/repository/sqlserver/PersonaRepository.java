package cl.previred.personas.repository.sqlserver;

import cl.previred.personas.domain.entity.sqlserver.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * El RUT es la clave primaria de {@link Persona}, asi
 * que {@code existsById}/{@code findById} de {@link JpaRepository} ya cubren las
 * consultas por RUT sin necesidad de metodos derivados adicionales.
 */
public interface PersonaRepository extends JpaRepository<Persona, String> {
}
