package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;

import java.time.LocalDateTime;

public record VisitDetailResponse(

        Long visitId,

        String visitReference,

        VisitorDetails visitor,

        VisitorType visitorType,

        RegistrationType registrationType,

        String purpose,

        HostDetails host,

        LocalDateTime expectedArrivalAt,

        LocalDateTime expectedDepartureAt,

        String remarks,

        ProofDetails proof,

        VisitStatus status,

        AuditDetails audit

) {

    public record VisitorDetails(

            Long visitorId,

            String firstName,

            String lastName,

            String email,

            String mobileNumber,

            String companyName

    ) {
    }

    public record HostDetails(

            Long hostId,

            String hostName,

            Long departmentId,

            String departmentName

    ) {
    }

    public record ProofDetails(

            String proofType,

            String proofNumber,

            String proofImagePath

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