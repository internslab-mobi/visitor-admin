package com.adminvisitor.service.impl;

import com.adminvisitor.service.QrMicroserviceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class QrMicroserviceClientImpl implements QrMicroserviceClient {

    private final RestClient restClient;

    public QrMicroserviceClientImpl(
            @Value("${qr.service.base-url}") String baseUrl) {

        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public String generateToken() {

        Map<String, Object> response = restClient.post()
                .uri("/api/qr/token")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("token") == null) {
            throw new IllegalStateException(
                    "QR microservice returned no token"
            );
        }

        log.info("QR_TOKEN_GENERATION_SUCCEEDED");

        return response.get("token").toString();
    }

    @Override
    public String generateQrCode(String payload) {

        Map<String, String> request = Map.of(
                "payload", payload
        );

        Map<String, Object> response = restClient.post()
                .uri("/api/qr/generate")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("qrCode") == null) {
            throw new IllegalStateException(
                    "QR microservice returned no QR image"
            );
        }

        log.info("QR_CODE_GENERATION_SUCCEEDED");

        return response.get("qrCode").toString();
    }
}