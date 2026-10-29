package com.adminvisitor.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Types of visitors supported by the VMS application")
public enum VisitorType {

    @Schema(description = "Guest visiting the organization")
    GUEST,

    @Schema(description = "Visitor attending the organization for a meeting or other purpose")
    VISITOR,

    @Schema(description = "Vendor visiting the organization for business or service-related purposes")
    VENDOR
}
