package com.adminvisitor.dto.requestdto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckInRequest(

        @Pattern(
                regexp = "^\\d{12}$",
                message = "Aadhaar number must contain exactly 12 digits"
        )
        String aadharNumber,

        @Pattern(
                regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
                message = "PAN number must be in valid format (ABCDE1234F)"
        )
        String panNumber,

        @Size(
                min = 6,
                max = 20,
                message = "Passport number must be between 6 and 20 characters"
        )
        String passportNumber

) {
}