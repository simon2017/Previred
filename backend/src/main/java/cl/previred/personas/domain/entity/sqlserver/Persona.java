package cl.previred.personas.domain.entity.sqlserver;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * El RUT es la clave primaria: en Chile identifica de forma unica y natural a una
 * persona, asi que no tiene sentido introducir un identificador artificial (UUID)
 * adicional. Esto tambien simplifica el mecanismo de contingencia (ver
 * {@link cl.previred.personas.resilience.OutboxService}): el mismo RUT normalizado
 * identifica al registro tanto en SQL Server como en el outbox local, sin necesidad de
 * generar ni remapear un id aparte al reconciliar.
 * <p>
 * Al ser la PK, el RUT se trata como inmutable una vez creado el registro (ver
 * {@link cl.previred.personas.service.PersonaServiceImpl#actualizar}).
 */
@Entity
@Table(name = "personas")
@Getter
public class Persona {

    @Id
    @Column(length = 12)
    private String rut;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Embedded
    private Direccion direccion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Persona() {
        // requerido por JPA
    }

    public Persona(String rut, String nombre, String apellido, LocalDate fechaNacimiento, Direccion direccion) {
        this.rut = rut;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
        Instant ahora = Instant.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    /** Reemplaza los datos editables de la persona. El RUT nunca cambia (ver javadoc de la clase). */
    public void actualizarDatos(String nombre, String apellido, LocalDate fechaNacimiento, Direccion direccion) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
    }

    /** Hook de JPA: refresca {@code updatedAt} justo antes de cada UPDATE. */
    @PreUpdate
    void alActualizar() {
        this.updatedAt = Instant.now();
    }
}
