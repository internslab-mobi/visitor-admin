package com.adminvisitor.dto.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlacklistRequest {

    @NotNull(message = "Visitor ID is required")
    @Positive(message = "Visitor ID must be positive")
    private Long visitorId;

    @NotBlank(message = "ID type is required")
    @Size(max = 50, message = "ID type cannot exceed 50 characters")
    private String idType;

    @NotBlank(message = "ID number is required")
    @Size(min = 3, max = 100, message = "ID number must be between 3 and 100 characters")
    private String idNumber;

    @NotBlank(message = "Reason is required")
    @Size(min = 3, max = 255, message = "Reason must be between 3 and 255 characters")
    private String reason;

    @NotNull(message = "Added by is required")
    @Positive(message = "Added by must be positive")
    private String createdBy;
}