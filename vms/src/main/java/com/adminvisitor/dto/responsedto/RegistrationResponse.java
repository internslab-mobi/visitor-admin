package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Response containing the result and details of a visitor registration")
public record RegistrationResponse(

        @Schema(
                description = "Unique identifier of the registered visit",
                example = "VIS-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String visitId,

        @Schema(
                description = "Reference number assigned to the visit",
                example = "VREF-20261022-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String visitReference,

        @Schema(
                description = "Unique identifier of the registered visitor",
                example = "VTR-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String visitorId,

        @Schema(
                description = "Visitor's first name",
                example = "Arun",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String firstName,

        @Schema(
                description = "Visitor's last name",
                example = "Kumar",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String lastName,

        @Schema(
                description = "Visitor's email address",
                example = "arun.kumar@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @Schema(
                description = "Visitor's mobile number",
                example = "9876543210",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String mobileNumber,

        @Schema(
                description = "Name of the visitor's company, if applicable",
                example = "ABC Technologies",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String companyName,

        @Schema(
                description = "Category of visitor",
                example = "VISITOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        VisitorType visitorType,

        @Schema(
                description = "Type of registration performed",
                example = "PRE_REGISTRATION",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        RegistrationType registrationType,

        @Schema(
                description = "Purpose of the visit",
                example = "Business meeting",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String purpose,

        @Schema(
                description = "ID of the employee hosting the visitor",
                example = "EMP-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String hostId,

        @Schema(
                description = "Expected arrival date and time",
                example = "2026-10-25T09:30:00",
                type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDateTime expectedArrivalAt,

        @Schema(
                description = "Expected departure date and time",
                example = "2026-10-25T10:30:00",
                type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDateTime expectedDepartureAt,

        @Schema(
                description = "Additional remarks about the visit, if any",
                example = "Please contact the host upon arrival",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String remarks,

        @Schema(
                description = "Current status of the visit",
                example = "REGISTERED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        VisitStatus status,

        @Schema(
                description = "Message describing the registration result",
                example = "Visitor registered successfully",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message
) {
}