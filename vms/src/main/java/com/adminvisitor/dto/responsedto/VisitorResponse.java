package com.adminvisitor.dto.responsedto;

import java.time.LocalDateTime;

public record VisitorResponse(

        String id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String companyName
       // LocalDateTime validity

) {
}