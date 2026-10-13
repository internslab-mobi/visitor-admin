package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.enums.UserRole;

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
