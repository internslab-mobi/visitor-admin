package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BlacklistStatus;

import java.time.LocalDateTime;

public record BlacklistResponse(
        String id,
        String visitorId,
        String reason,
        BlacklistStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy
) {
}