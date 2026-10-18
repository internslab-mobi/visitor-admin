package com.qrservice.dto;


import jakarta.validation.constraints.NotBlank;

public record QrGenerateRequest(
        @NotBlank(message = "Payload must not be empty")
        String payload
) {}