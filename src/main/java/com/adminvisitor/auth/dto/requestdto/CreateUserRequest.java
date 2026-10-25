package com.adminvisitor.auth.dto.requestdto;

import com.adminvisitor.auth.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a user account for an existing employee.
 *
 * @param employeeId the unique identifier of the existing employee
 * @param role the role assigned to the new user account
 * @author Logeshwaran
 */
@Schema(
        name = "CreateUserRequest",
        description = "Request payload for creating a user account"
)
public record CreateUserRequest(

        @Schema(
                description = "Unique identifier of the existing employee",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Employee ID is required")
        String employeeId,

        @Schema(
                description = "Role assigned to the new user account",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "User role is required")
        UserRole role

) {
}