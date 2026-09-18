package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BadgeStatus;

public record QrValidationResponse(
        BadgeStatus status,
        String message
) {
}