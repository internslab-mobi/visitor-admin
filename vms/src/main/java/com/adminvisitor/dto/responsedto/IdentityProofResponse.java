package com.adminvisitor.dto.responsedto;

public record IdentityProofResponse(
        String documentId,
        String visitorId,
        String nationality,
        String aadharNumber,
        String panNumber,
        String passportNumber
) {
}