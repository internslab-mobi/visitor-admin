package com.adminvisitor.dto.responsedto;

public record DocumentResponse(
        String documentId,
        String documentPath,
        String createdAt,
        String validFrom,
        String validUntil,
        String overwrittenBy
) {
}