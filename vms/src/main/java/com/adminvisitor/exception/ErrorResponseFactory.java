package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;
import com.adminvisitor.util.TraceIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final TraceIdProvider traceIdProvider;

    public ErrorResponse create(ErrorCode code, String path) {
        return create(code, code.getDefaultMessage(), path, null);
    }

    public ErrorResponse create(ErrorCode code, String message, String path,
                                Map<String, String> fieldErrors) {
        return build(code.getHttpStatus(), code.name(), message, path, fieldErrors);
    }

    public ErrorResponse create(HttpStatus status, String path) {
        return build(status, status.name(), status.getReasonPhrase(), path, null);
    }

    private ErrorResponse build(HttpStatus status, String code, String message,
                                String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status.value(), code, message,
                path, traceIdProvider.getCurrentTraceId(), fieldErrors);
    }
}