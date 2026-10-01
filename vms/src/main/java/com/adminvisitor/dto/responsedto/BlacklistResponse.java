package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.VisitorType;

import java.time.LocalDateTime;

public record BlacklistResponse(
        String id,
        String visitorId,
        String visitorName,
        String email,
        String mobileNumber,
        String companyName,
        Nationality nationality,
        String reason,
        BlacklistStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy,
        VisitorType visitorType
) {
}