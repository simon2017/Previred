package cl.previred.personas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * La autoconfiguracion por defecto de datasource/JPA de Spring Boot se excluye a
 * proposito: la aplicacion maneja dos fuentes de datos independientes (SQL Server y el
 * almacen local de contingencia en H2), cada una configurada explicitamente en
 * {@link cl.previred.personas.config.SqlServerPersistenceConfig} y
 * {@link cl.previred.personas.config.OutboxPersistenceConfig}.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class PersonasApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonasApplication.class, args);
    }
}
