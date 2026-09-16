package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BlacklistStatus;

import java.time.LocalDateTime;

public record BlacklistResponse(
        Long id,
        Long visitorId,
        String idType,
        String idNumber,
        String reason,
        BlacklistStatus status,
        Long addedBy,
        LocalDateTime addedAt,
        LocalDateTime removedAt,
        Long removedBy
) {
}