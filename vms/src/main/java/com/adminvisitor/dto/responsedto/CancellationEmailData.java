package com.adminvisitor.dto.responsedto;

public record CancellationEmailData(
        String visitorName,
        String visitorEmail,
        String visitReference,
        String purpose,
        String expectedArrival,
        String expectedDeparture,
        String hostName
) {
}