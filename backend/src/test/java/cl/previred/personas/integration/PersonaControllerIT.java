package cl.previred.personas.integration;

import cl.previred.personas.domain.enums.EstadoSincronizacion;
import cl.previred.personas.dto.request.DireccionRequest;
import cl.previred.personas.dto.request.PersonaRequest;
import cl.previred.personas.dto.response.ErrorResponse;
import cl.previred.personas.dto.response.PersonaResponse;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case para validar CRUD y validar restricciones de rut.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PersonaControllerIT {
    // @Testcontainers ya administra el ciclo de vida, no requiere try-with-resources.
    @SuppressWarnings("resource")
    @Container
    static MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withPassword("Previred_2025!")
                    // Sin TLS: el contenedor de test no tiene certificado configurado.
                    .withUrlParam("encrypt", "false");

    /**
     * Inicializo variables requeridas para conexion a datasource del TestContainer
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
        registry.add("spring.flyway.url", sqlServer::getJdbcUrl);
        registry.add("spring.flyway.user", sqlServer::getUsername);
        registry.add("spring.flyway.password", sqlServer::getPassword);
        registry.add("app.datasource.outbox.url",
                () -> "jdbc:h2:mem:outbox-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private PersonaRequest requestValido(String rut, String apellido) {
        return new PersonaRequest(rut, "Juan", apellido, LocalDate.of(1990, 1, 1),
                new DireccionRequest("Calle 1", "Comuna", "Region"));
    }

    /**
     * Valida los casos de uso de :
     * 1- Crear C
     * 2- Leer y Listar R
     * 3- Actualizar U
     * 4- Eliminar D
     */
    @Test
    void crudCompletoFuncionaDeExtremoAExtremo() {
        String rut = "12345678-5";
        PersonaRequest request = requestValido(rut, "Perez");

        //Crear
        ResponseEntity<PersonaResponse> creada = restTemplate.postForEntity("/api/personas", request,
                PersonaResponse.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creada.getBody()).isNotNull();
        assertThat(creada.getBody().rut()).isEqualTo(rut);
        assertThat(creada.getBody().estadoSincronizacion()).isEqualTo(EstadoSincronizacion.SINCRONIZADO);

        //Leer
        ResponseEntity<PersonaResponse> obtenida = restTemplate.getForEntity("/api/personas/{rut}",
                PersonaResponse.class, rut);
        assertThat(obtenida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(obtenida.getBody()).isNotNull();
        assertThat(obtenida.getBody().edad()).isGreaterThanOrEqualTo(30);

        //Listar
        ResponseEntity<PersonaResponse[]> listado = restTemplate.getForEntity("/api/personas",
                PersonaResponse[].class);
        assertThat(listado.getBody()).extracting(PersonaResponse::rut).contains(rut);

        //Actualizar
        PersonaRequest actualizacion = new PersonaRequest(rut, "Juan", "Perez Actualizado",
                LocalDate.of(1990, 1, 1), new DireccionRequest("Calle 2", "Comuna", "Region"));
        restTemplate.put("/api/personas/{rut}", actualizacion, rut);

        ResponseEntity<PersonaResponse> actualizada = restTemplate.getForEntity("/api/personas/{rut}",
                PersonaResponse.class, rut);
        assertThat(actualizada.getBody()).isNotNull();
        assertThat(actualizada.getBody().apellido()).isEqualTo("Perez Actualizado");

        //Eliminar
        restTemplate.delete("/api/personas/{rut}", rut);

        ResponseEntity<ErrorResponse> despuesDeEliminar = restTemplate.getForEntity("/api/personas/{rut}",
                ErrorResponse.class, rut);
        assertThat(despuesDeEliminar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Caso de borde, no permite modificar el rut de una persona ya registrada
     */
    @Test
    void rechazaActualizacionQueIntentaCambiarElRut() {
        String rut = "76086428-5";
        restTemplate.postForEntity("/api/personas", requestValido(rut, "Original"), PersonaResponse.class);

        PersonaRequest conOtroRut = requestValido("11111111-1", "Otro");
        ResponseEntity<ErrorResponse> respuesta = restTemplate.exchange("/api/personas/{rut}",
                org.springframework.http.HttpMethod.PUT, new org.springframework.http.HttpEntity<>(conOtroRut),
                ErrorResponse.class, rut);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rechazaRutInvalidoConBadRequest() {
        PersonaRequest invalido = requestValido("11.111.111-2", "Perez");

        ResponseEntity<ErrorResponse> respuesta = restTemplate.postForEntity("/api/personas", invalido,
                ErrorResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Caso de borde, no permite duplicados de rut
     */
    @Test
    void rechazaRutDuplicadoConConflict() {
        PersonaRequest request = requestValido("6-K", "Duplicado");
        restTemplate.postForEntity("/api/personas", request, PersonaResponse.class);

        ResponseEntity<ErrorResponse> segundo = restTemplate.postForEntity("/api/personas", request,
                ErrorResponse.class);

        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
