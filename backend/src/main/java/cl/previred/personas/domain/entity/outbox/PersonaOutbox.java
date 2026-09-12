package cl.previred.personas.domain.entity.outbox;

import cl.previred.personas.domain.enums.EstadoOutbox;
import cl.previred.personas.domain.enums.TipoOperacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * Registro de contingencia: se crea cuando el guardado en SQL Server falla por
 * indisponibilidad. Vive unicamente en el almacen H2 local (ver
 * {@link cl.previred.personas.config.OutboxPersistenceConfig}), nunca en SQL Server.
 * <p>
 * Se identifica por el mismo RUT normalizado que tendra la {@code Persona} una vez
 * sincronizada (el RUT es la PK en ambos almacenes, ver
 * {@link cl.previred.personas.domain.entity.sqlserver.Persona}).
 */
@Entity
@Table(name = "persona_outbox")
@Getter
public class PersonaOutbox {

    @Id
    @Column(length = 12)
    private String rut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOperacion operacion;

    /** Snapshot en JSON del request original (ver PersonaMapper#toOutboxPayload). */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOutbox estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PersonaOutbox() {
        // requerido por JPA
    }

    /** Crea un registro nuevo en estado PENDING, listo para ser reconciliado. */
    public PersonaOutbox(String rut, TipoOperacion operacion, String payload) {
        this.rut = rut;
        this.operacion = operacion;
        this.payload = payload;
        this.estado = EstadoOutbox.PENDING;
        this.intentos = 0;
        Instant ahora = Instant.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    /** Reemplaza el snapshot JSON pendiente de sincronizar (ej. tras un PUT sobre el registro en contingencia). */
    public void actualizarPayload(String payload) {
        this.payload = payload;
        this.updatedAt = Instant.now();
    }

    /** Registra un intento fallido de sincronizacion sin agotar aun los reintentos. */
    public void registrarFallo(String error) {
        this.intentos++;
        this.ultimoError = truncar(error);
        this.updatedAt = Instant.now();
    }

    /** Marca el registro como FAILED: se agotaron los reintentos automaticos. */
    public void marcarFallidoDefinitivo(String error) {
        this.estado = EstadoOutbox.FAILED;
        this.intentos++;
        this.ultimoError = truncar(error);
        this.updatedAt = Instant.now();
    }

    /** Recorta el mensaje de error al limite de la columna {@code ultimo_error}. */
    private String truncar(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}
