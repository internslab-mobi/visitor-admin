package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.List;

public record VisitorEditResponse(

        VisitorInfo visitor,

        VendorInfo vendor,

        List<DocumentInfo> documents,

        List<VisitInfo> visits,

        BlacklistInfo blacklist,

        boolean blacklisted

) {

    public record VisitorInfo(
            String id,
            String firstName,
            String lastName,
            String email,
            String mobileNumber,
            String companyName,
            VisitorType visitorType,
            Nationality nationality
    ) {}

    public record VendorInfo(
            String vendorId
    ) {}

    public record DocumentInfo(
            String documentId,
            DocumentType documentType,
            String createdAt
    ) {}

    public record VisitInfo(
            String visitId,
            String visitReference,
            VisitorType visitorType,
            RegistrationType registrationType,
            String purpose,

            String hostId,
            String hostName,

            String departmentId,
            String departmentName,

            LocalDateTime expectedArrivalAt,
            LocalDateTime expectedDepartureAt,

            LocalDateTime checkedInAt,
            LocalDateTime checkedOutAt,

            String remarks,
            VisitStatus status
    ) {}

    public record BlacklistInfo(
            String id,
            String reason,
            BlacklistStatus status,
            LocalDateTime createdAt
    ) {}
}