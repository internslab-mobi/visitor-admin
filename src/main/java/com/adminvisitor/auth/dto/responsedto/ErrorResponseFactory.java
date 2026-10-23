package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.util.TraceIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Factory component responsible for creating standardized {@link ErrorResponse} instances
 * populated with current timestamps and distributed tracing identifiers.
 *
 * @author Logeshwaran
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final TraceIdProvider traceIdProvider;

    /**
     * Creates an {@link ErrorResponse} populated with the specified status, error code, message,
     * request path, and optional field validation errors.
     *
     * @param status the HTTP response status
     * @param code the machine-readable error code string
     * @param message the descriptive error message
     * @param path the request URI path
     * @param fieldErrors optional map of field-level validation errors
     * @return a populated {@link ErrorResponse} instance
     */
    public ErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                path,
                traceIdProvider.getCurrentTraceId(),
                fieldErrors
        );
    }
}