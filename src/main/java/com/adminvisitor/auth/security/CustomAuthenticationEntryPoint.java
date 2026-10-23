package com.adminvisitor.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Authentication entry point implementation invoked when an unauthenticated request targets a secured endpoint.
 * <p>
 * Writes a standardized JSON 401 Unauthorized response to the client indicating missing, invalid, or expired credentials.
 * </p>
 *
 * @author Logeshwaran
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Commences the authentication failure response by serializing a 401 JSON error payload.
     *
     * @param request the HTTP servlet request resulting in an authentication failure
     * @param response the HTTP servlet response to write the unauthorized status and message to
     * @param authException the exception that triggered the authentication failure
     * @throws IOException if an input or output exception occurs writing to the response writer
     * @throws ServletException if a servlet exception occurs
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        System.out.println(
                "AUTHENTICATION ENTRY POINT CALLED: "
                        + request.getMethod() + " "
                        + request.getRequestURI()
                        + " | Reason: " + authException.getMessage()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid, expired, or missing authentication token\"}",
                LocalDateTime.now()
        );

        response.getWriter().write(jsonResponse);
    }

}
