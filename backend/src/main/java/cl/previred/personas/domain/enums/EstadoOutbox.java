package cl.previred.personas.domain.enums;

/**
 * Estado interno de un registro en el almacen de contingencia. No incluye un estado
 * "sincronizado": un registro se elimina del outbox en cuanto se sincroniza
 * exitosamente con SQL Server, en vez de conservarse marcado (ver
 * {@link cl.previred.personas.resilience.OutboxService#eliminarSincronizado}).
 */
public enum EstadoOutbox {
    PENDING,
    /** Se agotaron los reintentos automaticos; requiere intervencion manual. */
    FAILED
}
