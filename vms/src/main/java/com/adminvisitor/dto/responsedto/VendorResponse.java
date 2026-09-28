package com.adminvisitor.dto.responsedto;


import java.time.LocalDateTime;

public record VendorResponse(
        String id,
        String visitorId,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String companyName,
        LocalDateTime validity
) {
}