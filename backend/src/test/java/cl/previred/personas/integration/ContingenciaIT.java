package cl.previred.personas.integration;

import cl.previred.personas.domain.enums.EstadoSincronizacion;
import cl.previred.personas.dto.request.DireccionRequest;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.PersonaResponse;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test estrella del desafio: "el registro debe quedar en la BD aunque esta no este
 * disponible en ese momento". Se interpone Toxiproxy entre la aplicacion y SQL Server
 * para poder cortar y restaurar la conexion de forma determinista (a diferencia de
 * detener/reiniciar el contenedor de SQL Server, que le reasigna el puerto).
 * <p>
 * El proxy se crea manualmente con {@code toxiproxy-java} (en vez de
 * {@code ToxiproxyContainer#getProxy}, que en las versiones recientes del modulo
 * testcontainers-toxiproxy quedo deprecado junto con toda la clase
 * {@code ContainerProxy}: el propio Javadoc de la libreria indica que el contenedor ya
 * no construye el cliente y que el proxy debe crearse a mano).
 * <p>
 * {@code MSSQLServerContainer} (a diferencia de {@code PostgreSQLContainer}) no
 * soporta crear una base de datos personalizada: siempre opera contra {@code master}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ContingenciaIT {

    private static final Network NETWORK = Network.newNetwork();
    private static final int PUERTO_PROXY_SQLSERVER = 8666;
    private static final String SA_PASSWORD = "Previred_2025!";

    // @Testcontainers ya administra el ciclo de vida, no requiere try-with-resources.
    @SuppressWarnings("resource")
    @Container
    static MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withPassword(SA_PASSWORD)
                    // Sin TLS: el contenedor de test no tiene certificado configurado.
                    .withUrlParam("encrypt", "false")
                    .withNetwork(NETWORK)
                    .withNetworkAliases("sqlserver");

    @SuppressWarnings("resource")
    @Container
    static ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(NETWORK);

    /** Proxy hacia SQL Server, usado para cortar/restaurar la conexion durante el test. */
    static Proxy proxy;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        try {
            ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
            proxy = toxiproxyClient.createProxy("sqlserver", "0.0.0.0:" + PUERTO_PROXY_SQLSERVER, "sqlserver:1433");
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible crear el proxy de Toxiproxy hacia SQL Server", e);
        }
        String jdbcUrlProxy = "jdbc:sqlserver://%s:%d;encrypt=false"
                .formatted(toxiproxy.getHost(), toxiproxy.getMappedPort(PUERTO_PROXY_SQLSERVER));

        registry.add("spring.datasource.url", () -> jdbcUrlProxy);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
        // Flyway migra conectandose directo al contenedor (sin pasar por el proxy),
        // para dejar el esquema listo antes de empezar a interferir la conexión.
        registry.add("spring.flyway.url", sqlServer::getJdbcUrl);
        registry.add("spring.flyway.user", sqlServer::getUsername);
        registry.add("spring.flyway.password", sqlServer::getPassword);
        registry.add("app.datasource.outbox.url",
                () -> "jdbc:h2:mem:outbox-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        // El scheduler de reconciliacion corre mas seguido en el perfil de test
        // (ver application-test.yml) para no alargar innecesariamente el test.
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void unRegistroCreadoConLaBdCaidaQuedaResguardadoYLuegoSeSincroniza() throws IOException {
        String rut = "11111111-1";
        PersonaRequest request = new PersonaRequest(rut, "Contingencia", "Test", LocalDate.of(1992, 2, 2),
                new DireccionRequest("Calle 1", "Comuna", "Region"));

        try {
            proxy.disable();

            ResponseEntity<PersonaResponse> creada = restTemplate.postForEntity("/api/personas", request,
                    PersonaResponse.class);
            assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(creada.getBody()).isNotNull();
            assertThat(creada.getBody().estadoSincronizacion())
                    .isEqualTo(EstadoSincronizacion.PENDIENTE_SINCRONIZACION);

            ResponseEntity<PersonaResponse[]> listado = restTemplate.getForEntity("/api/personas",
                    PersonaResponse[].class);
            assertThat(listado.getBody()).extracting(PersonaResponse::rut).contains(rut);

            ResponseEntity<PersonaResponse> obtenida = restTemplate.getForEntity("/api/personas/{rut}",
                    PersonaResponse.class, rut);
            assertThat(obtenida.getBody()).isNotNull();
            assertThat(obtenida.getBody().estadoSincronizacion())
                    .isEqualTo(EstadoSincronizacion.PENDIENTE_SINCRONIZACION);
        } finally {
            proxy.enable();
        }

        //esperamos unos segundos hasta que el scheduler reintente guardar el registro en la bd
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    ResponseEntity<PersonaResponse> sincronizada = restTemplate.getForEntity("/api/personas/{rut}",
                            PersonaResponse.class, rut);
                    assertThat(sincronizada.getBody()).isNotNull();
                    assertThat(sincronizada.getBody().estadoSincronizacion())
                            .isEqualTo(EstadoSincronizacion.SINCRONIZADO);
                });
    }
}
