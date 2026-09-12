package cl.previred.personas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata expuesta en /swagger-ui y /v3/api-docs para describir la API. */
@Configuration
public class OpenApiConfig {

    /** Define titulo, descripcion y version que se muestran en la documentacion generada. */
    @Bean
    public OpenAPI personasOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de Personas - Desafio Tecnico Previred 2025")
                .description("CRUD de personas con validaciones y resiliencia ante caida de la base de datos")
                .version("v1"));
    }
}
