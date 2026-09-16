package com.adminvisitor.dto.requestdto;

import com.adminvisitor.enums.VisitorType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record PreRegisterRequestDTO(

        @NotBlank(message = "Visitor name is required")
        @Size(
                min = 2,
                max = 150,
                message = "Visitor name must be between 2 and 150 characters"
        )
        String visitorName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(
                max = 254,
                message = "Email cannot exceed 254 characters"
        )
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Mobile number must be a valid 10-digit mobile number"
        )
        String mobile,

        @NotNull(message = "Visitor type is required")
        VisitorType visitorType,

        @NotBlank(message = "Company name is required")
        @Size(
                max = 150,
                message = "Company name cannot exceed 150 characters"
        )
        String companyName,

        @NotNull(message = "Visit date is required")
        LocalDate visitDate,

        @NotNull(message = "Expected check-in time is required")
        LocalTime expectedCheckIn,

        @NotNull(message = "Expected check-out time is required")
        LocalTime expectedCheckOut,

        @NotBlank(message = "Purpose of visit is required")
        @Size(
                min = 3,
                max = 500,
                message = "Purpose must be between 3 and 500 characters"
        )
        String purpose,

        @NotNull(message = "Host employee is required")
        @Positive(message = "Host employee ID must be positive")
        Long hostEmployeeId
) {
}