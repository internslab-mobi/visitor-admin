package com.adminvisitor.auth.dto.requestdto;

import com.adminvisitor.auth.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank(message = "Employee ID is required")
        String employeeId,

        @NotNull(message = "User role is required")
        UserRole role
) {
}
