package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;

public record VisitDashboardResponse(

        String visitId,
        String visitReference,
        String visitorId,
        String visitorName,
        String companyName,
        VisitorType visitorType,
        String purpose,
        String hostName,
        VisitStatus status

) {
}