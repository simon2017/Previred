package cl.previred.personas.domain.enums;

/** Estado de sincronizacion visible para el cliente de la API, no confundir con {@link EstadoOutbox}. */
public enum EstadoSincronizacion {
    SINCRONIZADO,
    PENDIENTE_SINCRONIZACION
}
