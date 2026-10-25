package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.util.TraceIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Factory component responsible for creating standardized {@link ErrorResponse}
 * instances with the current timestamp and distributed tracing identifier.
 *
 * @author Logeshwaran
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final TraceIdProvider traceIdProvider;

    /**
     * Creates a standardized {@link ErrorResponse} using the supplied error details.
     *
     * <p>Includes the current timestamp and the trace ID available from
     * {@link TraceIdProvider}. Field-level validation errors can be supplied
     * when applicable.</p>
     *
     * @param status the HTTP response status
     * @param code the machine-readable error code
     * @param message the human-readable error message
     * @param path the request URI path where the error occurred
     * @param fieldErrors the field-level validation errors, if applicable
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