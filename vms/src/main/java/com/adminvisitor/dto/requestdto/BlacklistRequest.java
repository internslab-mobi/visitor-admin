package com.adminvisitor.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to blacklist an existing visitor")
public record BlacklistRequest(

        @NotBlank(message = "Reason is required")
        @Size(
                min = 3,
                max = 255,
                message = "Reason must be between 3 and 255 characters"
        )
        @Schema(
                description = "Reason for blacklisting the visitor",
                example = "Violation of visitor policy",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String reason,

        @NotBlank(message = "Added by is required")
        @Schema(
                description = "Identifier or name of the user who added the visitor to the blacklist",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String createdBy
) {
}