package com.adminvisitor.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request containing the details required to create a department")
public record DepartmentCreateRequest(

        @NotBlank(message = "Department code is required")
        @Size(max = 50, message = "Department code must not exceed 50 characters")
        @Schema(
                description = "Unique code identifying the department",
                example = "IT",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentCode,

        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        @Schema(
                description = "Name of the department",
                example = "Information Technology",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentName
) {
}