package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Response containing visitor blacklist record details")
public record BlacklistResponse(

        @Schema(description = "Unique identifier of the blacklist record", example = "BL-001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String id,

        @Schema(description = "Unique identifier of the blacklisted visitor", example = "VTR-001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String visitorId,

        @Schema(description = "Full name of the visitor", example = "Arun Kumar",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String visitorName,

        @Schema(description = "Visitor's email address", example = "arun.kumar@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Visitor's mobile number", example = "9876543210",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String mobileNumber,

        @Schema(description = "Name of the visitor's company, if applicable",
                example = "ABC Technologies",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String companyName,

        @Schema(description = "Nationality classification of the visitor", example = "DOMESTIC",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Nationality nationality,

        @Schema(description = "Reason the visitor was blacklisted",
                example = "Repeated violation of visitor policies",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String reason,

        @Schema(description = "Current status of the blacklist record", example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BlacklistStatus status,

        @Schema(description = "ID of the user who created the blacklist record", example = "EMP-001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String createdBy,

        @Schema(description = "Date and time the blacklist record was created",
                example = "2026-10-22T10:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt,

        @Schema(description = "Date and time the blacklist record was last updated",
                example = "2026-10-22T11:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime updatedAt,

        @Schema(description = "ID of the user who last updated the blacklist record",
                example = "EMP-002",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String updatedBy,

        @Schema(description = "Category of the visitor", example = "VISITOR",
                requiredMode = Schema.RequiredMode.REQUIRED)
        VisitorType visitorType,

        @Schema(description = "Types of identity proof matching the blacklist record",
                example = "[\"AADHAAR\", \"PAN\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ProofType> matchingProofTypes
) {
}