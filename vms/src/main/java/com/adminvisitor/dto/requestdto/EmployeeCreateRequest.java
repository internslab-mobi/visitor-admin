package com.adminvisitor.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request containing the details required to create an employee")
public record EmployeeCreateRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        @Schema(
                description = "Employee's first name",
                example = "Arun",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        @Schema(
                description = "Employee's last name",
                example = "Kumar",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        @Schema(
                description = "Employee's email address",
                example = "arun.kumar@example.com",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @NotBlank(message = "Mobile number is required")
        @Size(max = 20, message = "Mobile number must not exceed 20 characters")
        @Schema(
                description = "Employee's mobile number, including an optional country code",
                example = "+919876543210",
                maxLength = 20,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String mobileNumber,

        @NotBlank(message = "Department is required")
        @Schema(
                description = "ID of the department to which the employee belongs",
                example = "DT-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String departmentId,

        @NotBlank(message = "Designation is required")
        @Size(max = 100, message = "Designation must not exceed 100 characters")
        @Schema(
                description = "Employee's job designation",
                example = "Software Engineer",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String designation
) {
}