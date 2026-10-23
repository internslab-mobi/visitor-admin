package com.adminvisitor.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Types of documents managed by the VMS application")
public enum DocumentType {

    @Schema(description = "Original NDA document uploaded for a visitor")
    NDA,

    @Schema(description = "Photograph associated with a visitor")
    VISITOR_PHOTO,

    @Schema(description = "Supporting document for an NDA validity extension of up to 11 months")
    NDA_SUPPORTING,

    @Schema(description = "NDA renewal document for an NDA validity extension from 1 to 5 yrs")
    NDA_RENEWAL
}