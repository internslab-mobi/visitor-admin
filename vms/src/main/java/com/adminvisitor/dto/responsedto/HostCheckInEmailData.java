package com.adminvisitor.dto.responsedto;

public record HostCheckInEmailData(
        String hostEmail,
        String hostName,
        String visitorName,
        String visitorType,
        String companyName,
        String visitReference,
        String purpose,
        String checkedInAt
) {}
