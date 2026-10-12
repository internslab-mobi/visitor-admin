package com.adminvisitor.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String requestId,
        Map<String, String> fieldErrors
) {
}
