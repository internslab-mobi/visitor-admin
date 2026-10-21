package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RegistrationResponse(

        String visitId,
        String visitReference,
        String visitorId,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String companyName,
        VisitorType visitorType,
        RegistrationType registrationType,
        String purpose,
        String hostId,
        LocalDateTime expectedArrivalAt,
        LocalDateTime expectedDepartureAt,
        String remarks,
        VisitStatus status,
        String message
) {
}