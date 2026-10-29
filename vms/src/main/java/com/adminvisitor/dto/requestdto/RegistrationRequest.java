package com.adminvisitor.dto.requestdto;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Request containing visitor details and visit scheduling information for registration")
public record RegistrationRequest(

        @NotNull(message = "Registration type is required")
        @Schema(
                description = "Type of registration being performed",
                example = "PRE_REGISTRATION",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        RegistrationType registrationType,

        @NotNull(message = "Visitor type is required")
        @Schema(
                description = "Category of visitor",
                example = "VISITOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        VisitorType visitorType,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        @Schema(
                description = "Visitor's first name",
                example = "Arun",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        @Schema(
                description = "Visitor's last name",
                example = "Kumar",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        @Schema(
                description = "Visitor's email address",
                example = "arun.kumar@example.com",
                maxLength = 254,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Mobile number must be a valid 10-digit number"
        )
        @Schema(
                description = "Visitor's 10-digit mobile number",
                example = "9876543210",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String mobileNumber,

        @Size(max = 150, message = "Company name cannot exceed 150 characters")
        @Schema(
                description = "Name of the visitor's company, if applicable",
                example = "ABC Technologies",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String companyName,

        @NotBlank(message = "Purpose of visit is required")
        @Size(
                min = 3,
                max = 500,
                message = "Purpose must be between 3 and 500 characters"
        )
        @Schema(
                description = "Purpose of the visit",
                example = "Business meeting",
                minLength = 3,
                maxLength = 500,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String purpose,

        @NotBlank(message = "Host employee is required")
        @Schema(
                description = "ID of the employee hosting the visitor",
                example = "EMP-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String hostId,

        @NotNull(message = "Visit date is required")
        @Schema(
                description = "Scheduled date of the visit",
                example = "2026-10-25",
                type = "string",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate visitDate,

        @NotNull(message = "Expected arrival time is required")
        @Schema(
                description = "Expected visitor arrival time",
                example = "09:30:00",
                type = "string",
                format = "time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalTime expectedArrivalTime,

        @NotNull(message = "Expected departure time is required")
        @Schema(
                description = "Expected visitor departure time",
                example = "10:30:00",
                type = "string",
                format = "time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalTime expectedDepartureTime,

        @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
        @Schema(
                description = "Additional remarks about the visit, if any",
                example = "Please contact the host upon arrival",
                maxLength = 1000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String remarks,

        @NotNull(message = "Nationality is required")
        @Schema(
                description = "Visitor's nationality classification",
                example = "DOMESTIC",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Nationality nationality,

        @Size(max = 50, message = "Aadhaar number cannot exceed 50 characters")
        @Schema(
                description = "Aadhaar number for DOMESTIC visitors, if provided",
                example = "123456789012",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String aadharNumber,

        // LocalDateTime validity,

        @Size(max = 50, message = "PAN number cannot exceed 50 characters")
        @Schema(
                description = "PAN number for DOMESTIC visitors, if provided",
                example = "ABCDE1234F",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String panNumber,

        @Size(max = 50, message = "Passport number cannot exceed 50 characters")
        @Schema(
                description = "Passport number for international visitors, if provided",
                example = "A1234567",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String passportNumber

) {
}