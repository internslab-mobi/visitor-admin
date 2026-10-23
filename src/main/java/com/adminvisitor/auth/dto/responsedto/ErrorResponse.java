package com.adminvisitor.auth.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API error response payload returned to clients upon errors or validation failures.
 *
 * @param timestamp the UTC timestamp when the error occurred
 * @param status the HTTP status code number
 * @param code machine-readable error code identifying the specific failure condition
 * @param message descriptive human-readable message explaining the error
 * @param path the request URI path where the error originated
 * @param traceId distributed tracing correlation identifier, when available
 * @param fieldErrors map of field names to validation error messages for form binding failures
 * @author Logeshwaran
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        Map<String, String> fieldErrors
) {
}