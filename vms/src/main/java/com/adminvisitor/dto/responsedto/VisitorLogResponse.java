package com.adminvisitor.dto.responsedto;

import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Response containing visitor and visit timing details for the visitor log")
public record VisitorLogResponse(

        @Schema(
                description = "Unique identifier of the visit",
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
                description = "Unique identifier of the visitor",
                example = "VTR-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String visitorId,

        @Schema(
                description = "Full name of the visitor",
                example = "Arun Kumar",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String visitorName,

        @Schema(
                description = "Category of visitor",
                example = "VISITOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        VisitorType visitorType,

        @Schema(
                description = "Name of the visitor's company, if applicable",
                example = "ABC Technologies",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String companyName,

        @Schema(
                description = "Full name of the employee hosting the visit",
                example = "Priya Sharma",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String hostName,

        @Schema(
                description = "Scheduled date of the visit",
                example = "2026-10-25",
                type = "string",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate visitDate,

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
                description = "Actual date and time the visitor checked in; null if not checked in",
                example = "2026-10-25T09:35:00",
                type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        LocalDateTime checkedInAt,

        @Schema(
                description = "Actual date and time the visitor checked out; null if not checked out",
                example = "2026-10-25T10:25:00",
                type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        LocalDateTime checkedOutAt,

        @Schema(
                description = "Current status of the visit",
                example = "CHECKED_OUT",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        VisitStatus status
) {
}