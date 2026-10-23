package com.adminvisitor.dto.requestdto;

import java.time.LocalDateTime;



public record QrBadgeRequest(
        String visitId,
        LocalDateTime expectedArrivalAt,
        String visitorId,
        LocalDateTime checkedInAt
) {
}
