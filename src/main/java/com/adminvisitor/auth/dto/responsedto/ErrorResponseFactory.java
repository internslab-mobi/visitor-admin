package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.util.TraceIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final TraceIdProvider traceIdProvider;

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