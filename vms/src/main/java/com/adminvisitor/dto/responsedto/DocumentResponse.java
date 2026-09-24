package com.adminvisitor.dto.responsedto;

public record DocumentResponse(
        String documentId,
        String documentPath,
        String createdAt
) {
}