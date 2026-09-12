package cl.previred.personas.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Properties;

/**
 * Fuente de datos principal: SQL Server, el almacen definitivo de personas.
 * <p>
 * Se configura manualmente (en vez de dejar la autoconfiguracion por defecto de
 * Spring Boot) para poder convivir de forma explicita con la segunda fuente de datos
 * de contingencia, ver {@link OutboxPersistenceConfig}.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "cl.previred.personas.repository.sqlserver",
        entityManagerFactoryRef = "sqlServerEntityManagerFactory",
        transactionManagerRef = "sqlServerTransactionManager"
)
public class SqlServerPersistenceConfig {

    /** Pool de conexiones principal contra SQL Server; marcado {@code @Primary} para toda la app. */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource sqlServerDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("sqlserver-pool");
        return dataSource;
    }

    /** EntityManagerFactory que escanea las entidades de negocio persistidas en SQL Server. */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean sqlServerEntityManagerFactory(
            @Qualifier("sqlServerDataSource") HikariDataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("cl.previred.personas.domain.entity.sqlserver");
        factory.setPersistenceUnitName("sqlserver");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);

        Properties props = new Properties();
        // El esquema lo administra Flyway (ver db/migration); Hibernate solo valida
        // que la entidad coincida con la tabla existente.
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.format_sql", "true");
        factory.setJpaProperties(props);
        return factory;
    }

    /** Transaction manager principal, usado por defecto salvo que se indique el del outbox. */
    @Bean
    @Primary
    public PlatformTransactionManager sqlServerTransactionManager(
            @Qualifier("sqlServerEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
