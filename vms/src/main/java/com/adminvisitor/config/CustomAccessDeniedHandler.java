package com.adminvisitor.config;

import com.adminvisitor.enums.ErrorCode;
import com.adminvisitor.exception.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;


@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    private final JsonMapper jsonMapper;

    public CustomAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        var status = errorCode.getHttpStatus();

        log.warn(
                "SECURITY_ACCESS_DENIED method={} path={} requestId={} principal={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("requestId"),
                request.getUserPrincipal() != null
                        ? request.getUserPrincipal().getName()
                        : "anonymous"
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorCode.name(),
                errorCode.getPublicMessage(),
                request.getRequestURI(),
                MDC.get("requestId"),
                null
        );

        jsonMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}

