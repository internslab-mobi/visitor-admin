package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;

import java.time.LocalDateTime;

public record BlacklistResponse(
        String id,
        String visitorId,
        Nationality nationality,
        String reason,
        BlacklistStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy
) {
}