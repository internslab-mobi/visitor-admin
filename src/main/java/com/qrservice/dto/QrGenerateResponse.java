package com.qrservice.dto;


public record QrGenerateResponse(
        String payload,
        String qrCode
) {
}