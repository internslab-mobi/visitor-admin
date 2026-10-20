package com.adminvisitor.auth.util;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {

    private final Tracer tracer;

    public TraceIdProvider(Tracer tracer) {
        this.tracer = tracer;
    }

    public String getCurrentTraceId() {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return null;
        }

        return currentSpan.context().traceId();
    }
}