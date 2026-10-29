package com.adminvisitor.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing department details")
public record DepartmentResponse(

        @Schema(
                description = "Unique identifier of the department",
                example = "DT-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String id,

        @Schema(
                description = "Unique code assigned to the department",
                example = "IT",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentCode,

        @Schema(
                description = "Name of the department",
                example = "Information Technology",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentName,

        @Schema(
                description = "Current status of the department",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status
) {
}