package com.adminvisitor.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for CSRF token access.
 *
 * @author Logeshwaran
 */
@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "Authentication and security-related APIs"
)
public class CsrfController {

    /**
     * Retrieves the CSRF token required for protected requests.
     *
     * @param csrfToken the CSRF token associated with the current request
     * @return the current CSRF token
     */
    @GetMapping("/csrf")
    @Operation(
            summary = "Get CSRF token",
            description = "Retrieves the CSRF token required for protected requests."
    )
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}