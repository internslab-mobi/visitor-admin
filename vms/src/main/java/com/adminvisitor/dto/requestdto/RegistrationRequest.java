package com.adminvisitor.dto.requestdto;

import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitorType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record RegistrationRequest(

        @NotNull(message = "Registration type is required")
        RegistrationType registrationType,

        @NotNull(message = "Visitor type is required")
        VisitorType visitorType,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Mobile number must be a valid 10-digit number"
        )
        String mobileNumber,

        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name cannot exceed 150 characters")
        String companyName,

        @NotBlank(message = "Purpose of visit is required")
        @Size(
                min = 3,
                max = 500,
                message = "Purpose must be between 3 and 500 characters"
        )
        String purpose,

        @NotBlank(message = "Host employee is required")
        String hostId,

        @NotNull(message = "Visit date is required")
        LocalDate visitDate,

        @NotNull(message = "Expected arrival time is required")
        LocalTime expectedArrivalTime,

        @NotNull(message = "Expected departure time is required")
        LocalTime expectedDepartureTime,

        @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
        String remarks,

        ProofType proofType,

        @Size(max = 100, message = "Proof number cannot exceed 100 characters")
        String proofNumber

) {
}