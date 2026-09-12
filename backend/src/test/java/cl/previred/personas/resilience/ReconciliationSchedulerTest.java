package cl.previred.personas.resilience;

import cl.previred.personas.domain.entity.outbox.PersonaOutbox;
import cl.previred.personas.domain.entity.sqlserver.Direccion;
import cl.previred.personas.domain.entity.sqlserver.Persona;
import cl.previred.personas.domain.enums.TipoOperacion;
import cl.previred.personas.dto.request.DireccionRequest;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.mapper.PersonaMapper;
import cl.previred.personas.repository.sqlserver.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Valida la funcionalidades del Scheduler para reconciliar personas
 **/
@SpringBootTest(classes = ReconciliationScheduler.class)
class ReconciliationSchedulerTest {

    private static final String RUT_OK = "12345678-5";
    private static final String RUT_FALLA = "6-K";

    @MockitoBean
    private OutboxService outboxService;

    @MockitoBean
    private PersonaRepository personaRepository;

    @MockitoBean
    private PersonaMapper mapper;

    @Autowired
    private ReconciliationScheduler scheduler;

    @Value("${app.resilience.reconciliation.max-intentos}")
    private int maxIntentos;

    /**
     * Si no hay pendientes, no ejecuta nada
     */
    @Test
    void siNoHayPendientes_noConsultaNiGuardaNada() {
        when(outboxService.obtenerPendientes()).thenReturn(List.of());

        scheduler.reconciliarPendientes();

        verifyNoInteractions(personaRepository);
    }

    /**
     * Cuando existe un registro pendiente de sincronizar a bd, el scheduler lo toma y lo carga a la bd
     */
    @Test
    void sincronizaUnRegistroPendienteExitosamente() {
        PersonaOutbox registro = new PersonaOutbox(RUT_OK, TipoOperacion.CREATE, "{payload}");
        PersonaRequest request = requestValido();
        Persona entidad = entidadValida();

        when(outboxService.obtenerPendientes()).thenReturn(List.of(registro));
        when(mapper.fromOutboxPayload(registro.getPayload())).thenReturn(request);
        when(mapper.toEntity(request)).thenReturn(entidad);

        scheduler.reconciliarPendientes();

        verify(personaRepository).save(entidad);
        verify(outboxService).eliminarSincronizado(RUT_OK);
        verify(outboxService, never()).registrarFallo(any(), any());
    }

    /**
     * Cuando hay un fallo procesando registros pendientes, no se bloquea el correcto funcionamiento del scheduler
     */
    @Test
    void unRegistroFallidoNoBloqueaLaSincronizacionDeOtro() {
        PersonaOutbox falla = new PersonaOutbox(RUT_FALLA, TipoOperacion.CREATE, "{malo}");
        PersonaOutbox ok = new PersonaOutbox(RUT_OK, TipoOperacion.CREATE, "{bueno}");
        PersonaRequest request = requestValido();
        Persona entidadOk = entidadValida();

        when(outboxService.obtenerPendientes()).thenReturn(List.of(falla, ok));
        when(mapper.fromOutboxPayload(falla.getPayload())).thenThrow(new IllegalStateException("payload corrupto"));
        when(mapper.fromOutboxPayload(ok.getPayload())).thenReturn(request);
        when(mapper.toEntity(request)).thenReturn(entidadOk);

        scheduler.reconciliarPendientes();

        verify(outboxService).registrarFallo(eq(RUT_FALLA), anyString());
        verify(personaRepository).save(entidadOk);
        verify(outboxService).eliminarSincronizado(RUT_OK);
    }

    /**
     * Al superar el maximo definido de reintentos lo marca como fallado para posterior procesamiento manual
     */
    @Test
    void alSuperarElMaximoDeReintentos_marcaFallidoDefinitivo() {
        // Ancla el test al valor real configurado en application.yml (documentado en el
        // README como 3), en vez de asumir un numero fijo que podria desincronizarse.
        assertThat(maxIntentos).isEqualTo(3);

        PersonaOutbox registro = new PersonaOutbox(RUT_OK, TipoOperacion.CREATE, "{payload}");
        for (int i = 0; i < maxIntentos - 1; i++) {
            registro.registrarFallo("fallo previo");
        }
        when(outboxService.obtenerPendientes()).thenReturn(List.of(registro));
        when(mapper.fromOutboxPayload(registro.getPayload())).thenThrow(new IllegalStateException("sigue fallando"));

        scheduler.reconciliarPendientes();

        verify(outboxService).marcarFallidoDefinitivo(eq(RUT_OK), anyString());
        verify(outboxService, never()).registrarFallo(eq(RUT_OK), anyString());
    }

    private PersonaRequest requestValido() {
        DireccionRequest direccion = new DireccionRequest("Calle 1", "Comuna", "Region");
        return new PersonaRequest("12.345.678-5", "Ana", "Diaz", LocalDate.of(1995, 3, 3), direccion);
    }

    private Persona entidadValida() {
        return new Persona(RUT_OK, "Ana", "Diaz", LocalDate.of(1995, 3, 3),
                new Direccion("Calle 1", "Comuna", "Region"));
    }
}
