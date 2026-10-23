package com.adminvisitor.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI 3 and Swagger documentation.
 * <p>
 * Defines API metadata and sets the server base URL pointing to the API Gateway.
 * </p>
 *
 * @author Logeshwaran
 */
@Configuration
public class OpenApiConfig {

    /**
     * Constructs and configures the {@link OpenAPI} specification bean with API Gateway server settings.
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI authOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8094")
                                .description("API Gateway")
                ));
    }
}
