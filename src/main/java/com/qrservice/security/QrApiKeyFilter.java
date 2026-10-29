package com.qrservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class QrApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-QR-API-KEY";

    private final String expectedApiKey;

    public QrApiKeyFilter(
            @Value("${qr.security.api-key}") String expectedApiKey) {

        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            throw new IllegalStateException(
                    "QR service API key must be configured");
        }

        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/actuator/health")
                || path.startsWith("/actuator/health/");
               // || (path.equals("/api/qr/process-scan")
                //&& request.getMethod().equalsIgnoreCase("POST"));
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println(
                "QR API KEY FILTER EXECUTED: "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
        );

        String suppliedApiKey = request.getHeader(API_KEY_HEADER);

        boolean keyMatches =
                suppliedApiKey != null
                        && !suppliedApiKey.isBlank()
                        && MessageDigest.isEqual(
                        expectedApiKey.getBytes(StandardCharsets.UTF_8),
                        suppliedApiKey.getBytes(StandardCharsets.UTF_8)
                );

        System.out.println("QR API KEY MATCHES: " + keyMatches);

        if (!keyMatches) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"message\":\"Invalid or missing API key\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

}