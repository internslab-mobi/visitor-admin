package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
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
        Long totalDurationSeconds,
        String remarks,
        VisitStatus status,
        AuditDetails audit

) {

    @Builder
    public record VisitorDetails(

            String visitorId,
            String firstName,
            String lastName,
            String email,
            String mobileNumber,
            String companyName,
            String nationality,
            boolean ndaAvailable,
            String ndaDocumentId

    ) {
    }

    @Builder
    public record HostDetails(

            String hostId,
            String hostName,
            String departmentId,
            String departmentName

    ) {
    }

    @Builder
    public record AuditDetails(

            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String createdBy,
            String updatedBy

    ) {
    }
}