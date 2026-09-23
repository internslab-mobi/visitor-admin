package com.adminvisitor.dto.responsedto;

public record DocumentResponse(
        String documentId,
       // String visitorId,
        String ndaDocument,
        String createdAt
) {
}