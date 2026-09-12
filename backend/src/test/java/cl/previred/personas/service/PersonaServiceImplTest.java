package cl.previred.personas.service;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.entity.sqlserver.Direccion;
import cl.previred.personas.domain.entity.sqlserver.Persona;
import cl.previred.personas.domain.enums.EstadoSincronizacion;
import cl.previred.personas.domain.enums.TipoOperacion;
import cl.previred.personas.dto.request.DireccionRequest;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.DireccionResponse;
import cl.previred.personas.dto.response.PersonaResponse;
import cl.previred.personas.exception.PersonaNotFoundException;
import cl.previred.personas.exception.RutDuplicadoException;
import cl.previred.personas.exception.RutInmutableException;
import cl.previred.personas.mapper.PersonaMapper;
import cl.previred.personas.repository.sqlserver.PersonaRepository;
import cl.previred.personas.resilience.OutboxService;
import cl.previred.personas.resilience.ResilienceExceptionClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casos de prueba para Persona Service
 */
@ExtendWith(MockitoExtension.class)
class PersonaServiceImplTest {

    private static final String RUT_VALIDO = "12345678-5";

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private PersonaMapper mapper;

    @Mock
    private ResilienceExceptionClassifier exceptionClassifier;

    private PersonaServiceImpl personaService;

    @BeforeEach
    void setUp() {
        personaService = new PersonaServiceImpl(personaRepository, outboxService, mapper, exceptionClassifier);
    }

    private PersonaRequest requestValido() {
        DireccionRequest direccion = new DireccionRequest("Calle 123", "Providencia", "Metropolitana");
        return new PersonaRequest("12.345.678-5", "Juan", "Perez", LocalDate.of(1990, 1, 1), direccion);
    }

    private Persona entidadValida() {
        return new Persona(RUT_VALIDO, "Juan", "Perez", LocalDate.of(1990, 1, 1),
                new Direccion("Calle 123", "Providencia", "Metropolitana"));
    }

    /**
     * Almacena correctamente la persona con la bd activa
     */
    @Test
    void crear_guardaEnSqlServerCuandoEstaDisponible() {
        PersonaRequest request = requestValido();
        Persona entidad = entidadValida();
        PersonaResponse esperado = new PersonaResponse(RUT_VALIDO, "Juan", "Perez", entidad.getFechaNacimiento(), 34,
                new DireccionResponse("Calle 123", "Providencia", "Metropolitana"),
                EstadoSincronizacion.SINCRONIZADO, null);

        when(mapper.toEntity(request)).thenReturn(entidad);
        when(personaRepository.existsById(RUT_VALIDO)).thenReturn(false);
        when(personaRepository.save(entidad)).thenReturn(entidad);
        when(mapper.toResponse(entidad, EstadoSincronizacion.SINCRONIZADO, null)).thenReturn(esperado);

        PersonaResponse resultado = personaService.crear(request);

        assertThat(resultado.estadoSincronizacion()).isEqualTo(EstadoSincronizacion.SINCRONIZADO);
        verify(outboxService, never()).encolarCreacion(eq(RUT_VALIDO), eq(request));
    }

    /**
     * Error cuando el rut esta duplicado
     */
    @Test
    void crear_cuandoRutYaExiste_lanzaRutDuplicadoException() {
        PersonaRequest request = requestValido();
        when(mapper.toEntity(request)).thenReturn(entidadValida());
        when(personaRepository.existsById(RUT_VALIDO)).thenReturn(true);

        assertThatThrownBy(() -> personaService.crear(request)).isInstanceOf(RutDuplicadoException.class);

        verify(personaRepository, never()).save(any());
        verify(outboxService, never()).encolarCreacion(eq(RUT_VALIDO), eq(request));
    }

    /**
     * Cuando hay falla en la conexion a bd, deja el registro guardado en H2 pendiente a ser registrado
     */
    @Test
    void crear_cuandoFallaConectividad_encolaEnOutboxYRetornaPendiente() {
        PersonaRequest request = requestValido();
        Persona entidad = entidadValida();
        DataAccessResourceFailureException fallo = new DataAccessResourceFailureException("sin conexion");
        PersonaResponse esperado = new PersonaResponse(RUT_VALIDO, "Juan", "Perez", entidad.getFechaNacimiento(), 34,
                new DireccionResponse("Calle 123", "Providencia", "Metropolitana"),
                EstadoSincronizacion.PENDIENTE_SINCRONIZACION, "mensaje");

        when(mapper.toEntity(request)).thenReturn(entidad);
        when(personaRepository.existsById(RUT_VALIDO)).thenReturn(false);
        when(personaRepository.save(entidad)).thenThrow(fallo);
        when(exceptionClassifier.esFallaDeConectividad(fallo)).thenReturn(true);
        when(mapper.toResponse(eq(entidad), eq(EstadoSincronizacion.PENDIENTE_SINCRONIZACION), anyString()))
                .thenReturn(esperado);

        PersonaResponse resultado = personaService.crear(request);

        assertThat(resultado.estadoSincronizacion()).isEqualTo(EstadoSincronizacion.PENDIENTE_SINCRONIZACION);
        verify(outboxService).encolarCreacion(RUT_VALIDO, request);
    }

    /**
     * Al intentar guardar, si ocurre un error que no es de conectividad, debe informar al solicitante
     */
    @Test
    void crear_cuandoErrorNoEsDeConectividad_sePropaga() {
        PersonaRequest request = requestValido();
        Persona entidad = entidadValida();
        RuntimeException otro = new RuntimeException("error inesperado");

        when(mapper.toEntity(request)).thenReturn(entidad);
        when(personaRepository.existsById(RUT_VALIDO)).thenReturn(false);
        when(personaRepository.save(entidad)).thenThrow(otro);
        when(exceptionClassifier.esFallaDeConectividad(otro)).thenReturn(false);

        assertThatThrownBy(() -> personaService.crear(request)).isSameAs(otro);
        verify(outboxService, never()).encolarCreacion(eq(RUT_VALIDO), eq(request));
    }

    /**
     * Lanzar error cuando al buscar por rut este no existe
     */
    @Test
    void obtenerPorRut_siNoExisteEnNingunLado_lanzaNotFound() {
        when(personaRepository.findById(RUT_VALIDO)).thenReturn(Optional.empty());
        when(outboxService.buscarPendientePorRut(RUT_VALIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personaService.obtenerPorRut(RUT_VALIDO))
                .isInstanceOf(PersonaNotFoundException.class);
    }

    /**
     * Lanzar error cuando se intenta modificar el rut de una persona ya registrada
     */
    @Test
    void actualizar_cuandoElRutDelBodyNoCoincideConLaUrl_lanzaRutInmutable() {
        DireccionRequest direccion = new DireccionRequest("Calle 1", "Comuna", "Region");
        PersonaRequest otroRut = new PersonaRequest("6-K", "Juan", "Perez", LocalDate.of(1990, 1, 1), direccion);

        assertThatThrownBy(() -> personaService.actualizar(RUT_VALIDO, otroRut))
                .isInstanceOf(RutInmutableException.class);
    }

    /**
     * Al solicitar la lista de personas creadas, se debe enviar las registradas y las pendientes de registrar (H2)
     */
    @Test
    void listar_combinaSincronizadasYPendientes() {
        Persona persona = entidadValida();
        PersonaResponse respSincronizada = new PersonaResponse(RUT_VALIDO, "Juan", "Perez",
                persona.getFechaNacimiento(), 34, new DireccionResponse("Calle 123", "Providencia", "Metropolitana"),
                EstadoSincronizacion.SINCRONIZADO, null);
        PersonaOutbox pendiente = new PersonaOutbox("1-9", TipoOperacion.CREATE, "{}");
        PersonaResponse respPendiente = new PersonaResponse("1-9", "Ana", "Diaz", LocalDate.of(2000, 1, 1), 26,
                new DireccionResponse("X", "Y", "Z"), EstadoSincronizacion.PENDIENTE_SINCRONIZACION, "msg");

        when(personaRepository.findAll()).thenReturn(List.of(persona));
        when(mapper.toResponse(persona, EstadoSincronizacion.SINCRONIZADO, null)).thenReturn(respSincronizada);
        when(outboxService.obtenerPendientes()).thenReturn(List.of(pendiente));
        when(mapper.toResponseFromOutbox(pendiente)).thenReturn(respPendiente);

        List<PersonaResponse> resultado = personaService.listar();

        assertThat(resultado).containsExactlyInAnyOrder(respSincronizada, respPendiente);
    }
}
