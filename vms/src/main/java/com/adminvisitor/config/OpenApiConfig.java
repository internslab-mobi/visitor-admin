package com.adminvisitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vmsOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8094/api/vms")
                                .description("API Gateway")
                ));
    }

    @Bean
    public org.springdoc.core.customizers.OpenApiCustomizer vmsApiPathCustomizer() {
        return openApi -> {
            var paths = openApi.getPaths();

            if (paths == null) {
                return;
            }

            var correctedPaths = new io.swagger.v3.oas.models.Paths();

            paths.forEach((path, pathItem) -> {
                String correctedPath = path.startsWith("/api/")
                        ? path.substring(4)
                        : path;

                correctedPaths.addPathItem(correctedPath, pathItem);
            });

            openApi.setPaths(correctedPaths);
        };
    }
}
