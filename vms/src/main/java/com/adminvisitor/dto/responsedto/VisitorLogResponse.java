package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record VisitorLogResponse(
        String visitId,
        String visitReference,
        String visitorId,
        String visitorName,
        VisitorType visitorType,
        String companyName,
        String hostName,
        LocalDate visitDate,
        LocalDateTime expectedArrivalAt,
        LocalDateTime expectedDepartureAt,
        LocalDateTime checkedInAt,
        LocalDateTime checkedOutAt,
        VisitStatus status
) {
}