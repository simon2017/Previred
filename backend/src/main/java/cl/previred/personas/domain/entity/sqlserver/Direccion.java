package cl.previred.personas.domain.entity.sqlserver;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Direccion de una {@link Persona}. Es un value object embebido: no tiene identidad ni
 * tabla propia, sus columnas se mapean directamente dentro de {@code personas}.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Direccion {

    @Column(nullable = false, length = 150)
    private String calle;

    @Column(nullable = false, length = 100)
    private String comuna;

    @Column(nullable = false, length = 100)
    private String region;

    public Direccion(String calle, String comuna, String region) {
        this.calle = calle;
        this.comuna = comuna;
        this.region = region;
    }
}
