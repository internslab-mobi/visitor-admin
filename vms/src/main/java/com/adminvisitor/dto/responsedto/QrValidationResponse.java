package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BadgeStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record QrValidationResponse(
        BadgeStatus status,
        String message,
        String visitorName,
        String visitReference,
        String hostName,
        LocalDateTime issuedAt,
        LocalDateTime validUntil
) {}