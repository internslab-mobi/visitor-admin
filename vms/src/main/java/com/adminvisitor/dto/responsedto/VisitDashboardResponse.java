package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;

import java.time.LocalDate;

public record VisitDashboardResponse(

        String visitId,
        String visitReference,

        String visitorId,
        String visitorName,
        String visitorEmail,
        String visitorMobile,
        String companyName,

        VisitorType visitorType,

        String purpose,
        String hostName,

        LocalDate lastVisit,

        VisitStatus status

) {
}