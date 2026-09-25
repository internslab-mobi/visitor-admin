package com.adminvisitor.dto.requestdto;

import jakarta.validation.constraints.Size;

public record CheckInRequest(

        @Size(
                max = 50,
                message = "Aadhaar number cannot exceed 50 characters"
        )
        String aadharNumber,

        @Size(
                max = 50,
                message = "PAN number cannot exceed 50 characters"
        )
        String panNumber,

        @Size(
                max = 50,
                message = "Passport number cannot exceed 50 characters"
        )
        String passportNumber

) {
}