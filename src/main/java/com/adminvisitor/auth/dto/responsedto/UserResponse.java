package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO containing the public profile details of a user account.
 *
 * @param userId the unique system-generated user identifier (e.g. USR-001)
 * @param employeeId the identifier of the associated employee record
 * @param email the user's primary email address
 * @param fullName the user's full name derived from the employee record
 * @param role the system access role assigned to the user
 * @param status the operational status of the account (e.g. ACTIVE)
 * @param mustChangePassword whether the user must change their password before standard access is granted
 * @author Logeshwaran
 */
@Schema(
        name = "UserResponse",
        description = "Response containing the public profile details of a user account"
)
public record UserResponse(

        @Schema(
                description = "Unique system-generated user identifier",
                example = "USR-001"
        )
        String userId,

        @Schema(
                description = "Identifier of the associated employee record",
                example = "EMP-001"
        )
        String employeeId,

        @Schema(
                description = "Primary email address of the user",
                example = "user@example.com"
        )
        String email,

        @Schema(
                description = "Full name of the user derived from the employee record",
                example = "John Smith"
        )
        String fullName,

        @Schema(
                description = "System access role assigned to the user",
                example = "ADMIN"
        )
        UserRole role,

        @Schema(
                description = "Current operational status of the account",
                example = "ACTIVE"
        )
        String status,

        @Schema(
                description = "Indicates whether the user must change their password before receiving standard access",
                example = "false"
        )
        boolean mustChangePassword

) {
}