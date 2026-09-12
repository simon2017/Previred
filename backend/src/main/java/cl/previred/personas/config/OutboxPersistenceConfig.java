package cl.previred.personas.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Properties;

/**
 * Fuente de datos de contingencia: un H2 embebido en archivo, independiente de la red
 * y de SQL Server. Cuando el guardado en SQL Server falla por indisponibilidad, el
 * registro se resguarda aqui (ver {@link cl.previred.personas.resilience.OutboxService})
 * y un job programado lo reintenta sincronizar
 * (ver {@link cl.previred.personas.resilience.ReconciliationScheduler}).
 * <p>
 * Se administra con su propio {@code DataSource}/{@code EntityManagerFactory}/
 * {@code PlatformTransactionManager}, de modo que una transaccion contra el outbox
 * nunca comparte recursos con una transaccion (fallida) contra SQL Server.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "cl.previred.personas.repository.outbox",
        entityManagerFactoryRef = "outboxEntityManagerFactory",
        transactionManagerRef = "outboxTransactionManager"
)
public class OutboxPersistenceConfig {

    /** Pool de conexiones dedicado al H2 de contingencia (independiente del pool de SQL Server). */
    @Bean
    public HikariDataSource outboxDataSource(
            @Value("${app.datasource.outbox.url}") String url,
            @Value("${app.datasource.outbox.username}") String username,
            @Value("${app.datasource.outbox.password}") String password,
            @Value("${app.datasource.outbox.driver-class-name}") String driverClassName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("outbox-pool");
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    /** EntityManagerFactory que solo escanea las entidades del outbox, aislado del de SQL Server. */
    @Bean
    public LocalContainerEntityManagerFactoryBean outboxEntityManagerFactory(
            @Qualifier("outboxDataSource") HikariDataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("cl.previred.personas.domain.entity.outbox");
        factory.setPersistenceUnitName("outbox");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        // Infraestructura tecnica interna (no es schema de negocio): se deja que
        // Hibernate mantenga el esquema del H2 de contingencia automaticamente.
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        factory.setJpaProperties(props);
        return factory;
    }

    /** Transaction manager propio del outbox: nunca participa de una transaccion contra SQL Server. */
    @Bean
    public PlatformTransactionManager outboxTransactionManager(
            @Qualifier("outboxEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
