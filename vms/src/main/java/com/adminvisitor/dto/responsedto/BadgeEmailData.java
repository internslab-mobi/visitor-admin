package com.adminvisitor.dto.responsedto;

public record BadgeEmailData(
        String visitorName,
        String visitorEmail,
        String visitReference,
        String hostName,
        String issuedAt,
        String validUntil,
        String qrCode,
        String visitorPhoto
) {}