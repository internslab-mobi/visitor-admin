package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;

public record VisitDashboardResponse(

        Long visitId,

        String visitReference,

        Long visitorId,

        String visitorName,

        String companyName,

        VisitorType visitorType,

        String purpose,

        String hostName,

        VisitStatus status

) {
}