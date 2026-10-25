package com.adminvisitor.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation.
 *
 * @author Logeshwaran
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the OpenAPI specification for the Auth application.
     *
     * @return the OpenAPI specification
     */
    @Bean
    public OpenAPI authOpenAPI() {
        return new OpenAPI();
    }
}
