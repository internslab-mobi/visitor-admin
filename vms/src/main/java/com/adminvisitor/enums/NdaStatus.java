package com.adminvisitor.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of an NDA document")
public enum NdaStatus {

    @Schema(description = "NDA is currently valid")
    ACTIVE,

    @Schema(description = "NDA validity period has ended")
    EXPIRED,

    @Schema(description = "NDA has been replaced by a newer document")
    SUPERSEDED
}
