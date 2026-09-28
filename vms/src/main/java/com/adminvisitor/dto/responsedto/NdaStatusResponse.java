package com.adminvisitor.dto.responsedto;

import java.time.LocalDateTime;

public record NdaStatusResponse(
        String visitorId,
        String visitorType,
        boolean ndaRequired,
        boolean ndaAvailable,
        LocalDateTime validUntil,
        String documentId
) {
}