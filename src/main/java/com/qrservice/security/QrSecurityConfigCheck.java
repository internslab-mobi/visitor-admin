package com.qrservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QrSecurityConfigCheck {

    public QrSecurityConfigCheck(
            @Value("${qr.security.api-key}") String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "QR_SERVICE_API_KEY is missing");
        }

        System.out.println("QR API key configuration loaded");
    }
}