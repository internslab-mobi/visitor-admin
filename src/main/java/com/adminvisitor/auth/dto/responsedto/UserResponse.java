package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.enums.UserRole;

/**
 * Data transfer object representing the public profile details of a user account.
 *
 * @param userId the unique system-generated user identifier (e.g. USR-001)
 * @param employeeId the identifier of the associated employee record
 * @param email the user's primary email address
 * @param fullName the user's full name derived from the employee record
 * @param role the system access role assigned to the user
 * @param status the operational status of the account (e.g. ACTIVE)
 * @param mustChangePassword whether the user must complete a password change before standard access is granted
 * @author Logeshwaran
 */
public record UserResponse(
        String userId,
        String employeeId,
        String email,
        String fullName,
        UserRole role,
        String status,
        boolean mustChangePassword
) {
}
