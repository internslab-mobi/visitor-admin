package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;

import java.time.LocalDateTime;

public record VisitDetailResponse(

        String visitId,
        String visitReference,
        VisitorDetails visitor,
        VisitorType visitorType,
        RegistrationType registrationType,
        String purpose,
        HostDetails host,
        LocalDateTime expectedArrivalAt,
        LocalDateTime expectedDepartureAt,
        LocalDateTime checkedInAt,
        LocalDateTime checkedOutAt,
        String remarks,
        VisitStatus status,
        AuditDetails audit

) {

    public record VisitorDetails(

            String visitorId,

            String firstName,

            String lastName,

            String email,

            String mobileNumber,

            String companyName,

            boolean ndaAvailable,
            String ndaDocumentId,
            LocalDateTime ndaValidUntil

    ) {
    }

    public record HostDetails(

            String hostId,

            String hostName,

            String departmentId,

            String departmentName

    ) {
    }

    public record AuditDetails(

            LocalDateTime createdAt,

            LocalDateTime updatedAt,

            String createdBy,

            String updatedBy

    ) {
    }
}