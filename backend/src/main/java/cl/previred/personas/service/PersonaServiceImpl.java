package cl.previred.personas.service;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.entity.sqlserver.Direccion;
import cl.previred.personas.domain.entity.sqlserver.Persona;
import cl.previred.personas.domain.enums.EstadoSincronizacion;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.PersonaResponse;
import cl.previred.personas.exception.PersonaNotFoundException;
import cl.previred.personas.exception.RutDuplicadoException;
import cl.previred.personas.exception.RutInmutableException;
import cl.previred.personas.exception.ServicioNoDisponibleException;
import cl.previred.personas.mapper.PersonaMapper;
import cl.previred.personas.repository.sqlserver.PersonaRepository;
import cl.previred.personas.resilience.OutboxService;
import cl.previred.personas.resilience.ResilienceExceptionClassifier;
import cl.previred.personas.util.RutUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementa los metodos CRUD para PersonaService.
 **/
@Service
public class PersonaServiceImpl implements PersonaService {

    private static final Logger log = LoggerFactory.getLogger(PersonaServiceImpl.class);

    private final PersonaRepository personaRepository;
    private final OutboxService outboxService;
    private final PersonaMapper mapper;
    private final ResilienceExceptionClassifier exceptionClassifier;

    public PersonaServiceImpl(PersonaRepository personaRepository, OutboxService outboxService,
                               PersonaMapper mapper, ResilienceExceptionClassifier exceptionClassifier) {
        this.personaRepository = personaRepository;
        this.outboxService = outboxService;
        this.mapper = mapper;
        this.exceptionClassifier = exceptionClassifier;
    }

    /**
     * Crea una persona. Si SQL Server no esta disponible, el registro se resguarda en el
     * outbox de contingencia en vez de perderse, y se responde igual con estado
     * {@code PENDIENTE_SINCRONIZACION}.
     */
    @Override
    public PersonaResponse crear(PersonaRequest request) {
        String rut = RutUtils.normalizar(request.rut());
        Persona persona = mapper.toEntity(request);
        try {
            if (personaRepository.existsById(rut)) {
                throw new RutDuplicadoException(rut);
            }
            Persona guardada = personaRepository.save(persona);
            return mapper.toResponse(guardada, EstadoSincronizacion.SINCRONIZADO, null);
        } catch (DataIntegrityViolationException ex) {
            // Cubre la carrera entre el chequeo existsById() y el save() bajo concurrencia.
            throw new RutDuplicadoException(rut);
        } catch (RutDuplicadoException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            rethrowSiNoEsFallaDeConectividad(ex);
            log.warn("SQL Server no disponible al crear la persona con rut {}; se resguarda en contingencia", rut, ex);
            outboxService.encolarCreacion(rut, request);
            return mapper.toResponse(persona, EstadoSincronizacion.PENDIENTE_SINCRONIZACION,
                    PersonaMapper.MENSAJE_CONTINGENCIA);
        }
    }

    /**
     * Lista todas las personas. Combina lo sincronizado en SQL Server con lo pendiente en
     * el outbox; si SQL Server no responde, se listan solo los registros en contingencia
     * en vez de fallar la operacion completa.
     */
    @Override
    public List<PersonaResponse> listar() {
        List<PersonaResponse> sincronizadas;
        try {
            sincronizadas = personaRepository.findAll().stream()
                    .map(p -> mapper.toResponse(p, EstadoSincronizacion.SINCRONIZADO, null))
                    .toList();
        } catch (RuntimeException ex) {
            rethrowSiNoEsFallaDeConectividad(ex);
            log.warn("SQL Server no disponible al listar personas; se muestran solo los registros en contingencia",
                    ex);
            sincronizadas = List.of();
        }
        List<PersonaResponse> pendientes = outboxService.obtenerPendientes().stream()
                .map(mapper::toResponseFromOutbox)
                .toList();
        return Stream.concat(sincronizadas.stream(), pendientes.stream())
                .sorted(Comparator.comparing(PersonaResponse::apellido).thenComparing(PersonaResponse::nombre))
                .toList();
    }

    /**
     * Busca una persona por RUT, primero en SQL Server y, si no esta ahi o SQL Server no
     * responde, en el outbox de contingencia.
     */
    @Override
    public PersonaResponse obtenerPorRut(String rut) {
        String rutNormalizado = RutUtils.normalizar(rut);
        try {
            Optional<Persona> persona = personaRepository.findById(rutNormalizado);
            if (persona.isPresent()) {
                return mapper.toResponse(persona.get(), EstadoSincronizacion.SINCRONIZADO, null);
            }
        } catch (RuntimeException ex) {
            rethrowSiNoEsFallaDeConectividad(ex);
            log.warn("SQL Server no disponible al buscar la persona con rut {}; se busca en contingencia",
                    rutNormalizado, ex);
        }
        return outboxService.buscarPendientePorRut(rutNormalizado)
                .map(mapper::toResponseFromOutbox)
                .orElseThrow(() -> new PersonaNotFoundException(rutNormalizado));
    }

    /**
     * Actualiza una persona existente. Si el registro aun esta pendiente en el outbox
     * se edita ahi directamente; si ya esta en SQL Server y este cae durante la
     * operacion, se informa un estado degradado (ver nota de alcance en el catch de
     * abajo) en vez de aplicar el cambio silenciosamente en otro lado.
     */
    @Override
    public PersonaResponse actualizar(String rut, PersonaRequest request) {
        String rutNormalizado = RutUtils.normalizar(rut);
        if (!rutNormalizado.equals(RutUtils.normalizar(request.rut()))) {
            throw new RutInmutableException();
        }

        Optional<PersonaOutbox> pendiente = outboxService.buscarPendientePorRut(rutNormalizado);
        if (pendiente.isPresent()) {
            // El registro aun no llega a SQL Server: se actualiza directamente en el outbox.
            outboxService.actualizarPendiente(rutNormalizado, request);
            Persona actualizada = mapper.toEntity(request);
            return mapper.toResponse(actualizada, EstadoSincronizacion.PENDIENTE_SINCRONIZACION,
                    PersonaMapper.MENSAJE_CONTINGENCIA);
        }
        try {
            Persona existente = personaRepository.findById(rutNormalizado)
                    .orElseThrow(() -> new PersonaNotFoundException(rutNormalizado));
            Direccion direccion = new Direccion(request.direccion().calle(), request.direccion().comuna(),
                    request.direccion().region());
            existente.actualizarDatos(request.nombre(), request.apellido(), request.fechaNacimiento(), direccion);
            Persona guardada = personaRepository.save(existente);
            return mapper.toResponse(guardada, EstadoSincronizacion.SINCRONIZADO, null);
        } catch (PersonaNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            rethrowSiNoEsFallaDeConectividad(ex);
            // Alcance de este desafio: la resiliencia de UPDATE/DELETE cubre registros
            // aun pendientes en el outbox (ver arriba); para un registro ya sincronizado
            // en SQL Server, si la BD cae en ese momento se informa el estado degradado
            // en vez de perder silenciosamente el cambio. Ver README, seccion supuestos.
            throw new ServicioNoDisponibleException(
                    "La base de datos no esta disponible en este momento; intenta actualizar este registro "
                            + "mas tarde.");
        }
    }

    /** Elimina una persona por RUT, ya sea que este pendiente en el outbox o ya sincronizada en SQL Server. */
    @Override
    public void eliminar(String rut) {
        String rutNormalizado = RutUtils.normalizar(rut);
        Optional<PersonaOutbox> pendiente = outboxService.buscarPendientePorRut(rutNormalizado);
        if (pendiente.isPresent()) {
            outboxService.eliminarPendiente(rutNormalizado);
            return;
        }
        try {
            if (personaRepository.findById(rutNormalizado).isEmpty()) {
                throw new PersonaNotFoundException(rutNormalizado);
            }
            personaRepository.deleteById(rutNormalizado);
        } catch (PersonaNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            rethrowSiNoEsFallaDeConectividad(ex);
            throw new ServicioNoDisponibleException(
                    "La base de datos no esta disponible en este momento; intenta eliminar este registro "
                            + "mas tarde.");
        }
    }

    /**
     * Punto unico donde se decide si una {@link RuntimeException} corresponde a una
     * falla de conectividad hacia SQL Server (en cuyo caso se deja continuar hacia la
     * ruta de contingencia) o si es un error de negocio que debe propagarse tal cual.
     */
    private void rethrowSiNoEsFallaDeConectividad(RuntimeException ex) {
        if (!exceptionClassifier.esFallaDeConectividad(ex)) {
            throw ex;
        }
    }
}
