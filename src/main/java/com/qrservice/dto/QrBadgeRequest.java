package com.qrservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record QrBadgeRequest(
        @NotBlank String visitId,
        @NotNull LocalDateTime expectedArrivalAt,
        @NotBlank String visitorId,
        @NotNull LocalDateTime checkedInAt
) {
}