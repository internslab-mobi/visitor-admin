package com.adminvisitor.auth.dto.requestdto;

import com.adminvisitor.auth.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object representing an administrative request to provision a new user account.
 *
 * @param employeeId the unique identifier of the existing, active employee
 * @param role the user role assigned to the new account
 * @author Logeshwaran
 */
public record CreateUserRequest(
        @NotBlank(message = "Employee ID is required")
        String employeeId,

        @NotNull(message = "User role is required")
        UserRole role
) {
}
