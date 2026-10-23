package com.adminvisitor.auth.util;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

/**
 * Utility component that extracts the current distributed tracing trace identifier.
 * <p>
 * Reads the trace ID from the active Micrometer {@link Tracer} span context for inclusion
 * in API error responses and diagnostic logs.
 * </p>
 *
 * @author Logeshwaran
 */
@Component
public class TraceIdProvider {

    private final Tracer tracer;

    /**
     * Constructs the trace ID provider with the configured Micrometer {@link Tracer}.
     *
     * @param tracer the distributed tracer bean
     */
    public TraceIdProvider(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Retrieves the current trace ID from the active span context.
     *
     * @return the hexadecimal trace ID string, or {@code null} if no active span is present
     */
    public String getCurrentTraceId() {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return null;
        }

        return currentSpan.context().traceId();
    }
}