package com.adminvisitor.dto.responsedto;

public record DocumentResponse(
        String documentId,
        String documentPath,
        String createdAt,
        String effective_validity,
        String validUntil,
        String overwrittenBy,
        String status
) {
}