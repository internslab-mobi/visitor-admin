package com.adminvisitor.auth.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API error response returned to clients when an error
 * or validation failure occurs.
 *
 * <p>Provides consistent error details to help clients identify the failure
 * and correlate it with application logs when a trace ID is available.</p>
 *
 * @param timestamp the UTC timestamp when the error occurred
 * @param status the HTTP status code
 * @param code machine-readable error code identifying the failure condition
 * @param message human-readable description of the error
 * @param path the request URI path where the error occurred
 * @param traceId distributed tracing correlation identifier, when available
 * @param fieldErrors map of field names to validation error messages, when applicable
 * @author Logeshwaran
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "ErrorResponse",
        description = "Standardized error response returned by the API"
)
public record ErrorResponse(

        @Schema(
                description = "UTC timestamp when the error occurred",
                example = "2026-10-25T02:30:00Z"
        )
        Instant timestamp,

        @Schema(
                description = "HTTP status code",
                example = "400"
        )
        int status,

        @Schema(
                description = "Machine-readable code identifying the error",
                example = "VALIDATION_ERROR"
        )
        String code,

        @Schema(
                description = "Human-readable description of the error",
                example = "Request validation failed"
        )
        String message,

        @Schema(
                description = "Request URI path where the error occurred",
                example = "/api/auth/login"
        )
        String path,

        @Schema(
                description = "Distributed tracing correlation identifier, when available",
                example = "4bf92f3577b34da6a3ce929d0e0e4736"
        )
        String traceId,

        @Schema(
                description = "Validation error messages mapped to their corresponding field names",
                example = "{\"email\":\"Invalid email format\",\"password\":\"Password is required\"}"
        )
        Map<String, String> fieldErrors

) {
}