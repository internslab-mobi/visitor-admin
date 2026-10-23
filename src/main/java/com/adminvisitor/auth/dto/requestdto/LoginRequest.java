package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object representing user authentication credentials submitted for login.
 *
 * @param email the user's registered email address
 * @param password the user's plaintext password
 * @author Logeshwaran
 */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {
}