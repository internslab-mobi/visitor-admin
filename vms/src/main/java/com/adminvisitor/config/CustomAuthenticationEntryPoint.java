package com.adminvisitor.config;

import com.adminvisitor.enums.ErrorCode;
import com.adminvisitor.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    private final JsonMapper jsonMapper;

    public CustomAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }


    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        var status = errorCode.getHttpStatus();

        log.warn(
                "SECURITY_AUTHENTICATION_FAILED method={} path={} requestId={} exception={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("requestId"),
                authException.getClass().getSimpleName()
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

