package com.adminvisitor.dto.requestdto;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlacklistRequest {

    @NotBlank(message = "Visitor ID is required")
    private String visitorId;

    @NotNull(message = "Nationality is required")
    private Nationality nationality;

    @NotNull(message = "Proof type is required")
    private ProofType proofType;

    @NotBlank(message = "Proof number is required")
    private String proofNumber;

    @NotBlank(message = "Reason is required")
    @Size(min = 3, max = 255, message = "Reason must be between 3 and 255 characters")
    private String reason;

    @NotBlank(message = "Added by is required")
    private String createdBy;
}