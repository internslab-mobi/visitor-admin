package com.adminvisitor.dto.responsedto;

import java.time.LocalDateTime;

public record VisitorResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String companyName,
        LocalDateTime cooldownUntil,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}