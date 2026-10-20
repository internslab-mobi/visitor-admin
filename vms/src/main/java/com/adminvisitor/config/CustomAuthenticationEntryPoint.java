package com.adminvisitor.config;

import com.adminvisitor.enums.ErrorCode;
import com.adminvisitor.exception.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.warn(
                "SECURITY_AUTHENTICATION_FAILED method={} path={} exception={}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getClass().getSimpleName()
        );

        errorResponseWriter.write(
                request,
                response,
                ErrorCode.UNAUTHORIZED
        );
    }
}
