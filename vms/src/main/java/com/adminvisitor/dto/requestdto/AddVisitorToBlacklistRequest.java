package com.adminvisitor.dto.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddVisitorToBlacklistRequest {

    @NotBlank(message = "Reason is required")
    @Size(
            min = 3,
            max = 255,
            message = "Reason must be between 3 and 255 characters"
    )
    private String reason;

    @NotBlank(message = "Added by is required")
    private String createdBy;
}