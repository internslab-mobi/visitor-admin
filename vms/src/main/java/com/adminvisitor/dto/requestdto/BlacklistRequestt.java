//package com.adminvisitor.dto.requestdto;
//
//import com.adminvisitor.enums.Nationality;
//import com.adminvisitor.enums.ProofType;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//
//@Schema(description = "Request containing the visitor details and proof required to add a visitor to the blacklist")
//public record BlacklistRequest(
//
//        @NotBlank(message = "Visitor ID is required")
//        @Schema(
//                description = "Unique ID of the visitor to blacklist",
//                example = "VTR-001",
//                requiredMode = Schema.RequiredMode.REQUIRED
//        )
//        String visitorId,
//
//        @NotNull(message = "Nationality is required")
//        @Schema(
//                description = "Nationality of the visitor",
//                example = "DOMESTIC",
//                requiredMode = Schema.RequiredMode.REQUIRED
//        )
//        Nationality nationality,
//
//        @NotNull(message = "Proof type is required")
//        @Schema(
//                description = "Type of identity proof provided by the visitor",
//                example = "AADHAAR",
//                requiredMode = Schema.RequiredMode.REQUIRED
//        )
//        ProofType proofType,
//
//        @NotBlank(message = "Proof number is required")
//        @Schema(
//                description = "Identity proof number of the visitor",
//                example = "123456789012",
//                requiredMode = Schema.RequiredMode.REQUIRED
//        )
//        String proofNumber,
//
//        @NotBlank(message = "Reason is required")
//        @Size(
//                min = 3,
//                max = 255,
//                message = "Reason must be between 3 and 255 characters"
//        )
//        @Schema(
//                description = "Reason for blacklisting the visitor",
//                example = "Repeated violation of visitor policies",
//                minLength = 3,
//                maxLength = 255,
//                requiredMode = Schema.RequiredMode.REQUIRED
//        )
//        String reason,
//
//        @NotBlank(message = "Added by is required")
//        @Schema(
//                description = "ID of the employee or administrator adding the visitor to the blacklist",
//                example = "EMP-001",
//                requiredMode = Schema.RequiredMode.REQUIRED
//        )
//        String createdBy
//) {
//}