package com.adminvisitor.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing employee details")
public record EmployeeResponse(

        @Schema(
                description = "Unique identifier of the employee",
                example = "EMP-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String id,

        @Schema(
                description = "Employee's first name",
                example = "Arun",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String firstName,

        @Schema(
                description = "Employee's last name",
                example = "Kumar",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String lastName,

        @Schema(
                description = "Employee's email address",
                example = "arun.kumar@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @Schema(
                description = "Employee's mobile number",
                example = "+919876543210",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String mobileNumber,

        @Schema(
                description = "ID of the department to which the employee belongs",
                example = "DT-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentId,

        @Schema(
                description = "Name of the employee's department",
                example = "Information Technology",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentName,

        @Schema(
                description = "Employee's job designation",
                example = "Software Engineer",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String designation,

        @Schema(
                description = "Current status of the employee",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status
) {
}