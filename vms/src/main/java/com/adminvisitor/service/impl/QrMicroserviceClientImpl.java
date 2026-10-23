
package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.QrBadgeResponse;
import com.adminvisitor.exception.ServiceUnavailableException;
import com.adminvisitor.service.QrMicroserviceClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Service
@Slf4j
public class QrMicroserviceClientImpl implements QrMicroserviceClient {

    private final RestClient restClient;

    public QrMicroserviceClientImpl(
            @Value("${qr.service.base-url}") String baseUrl,
            @Value("${qr.service.api-key}") String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "QR service API key must be configured"
            );
        }

        this.restClient = RestClient.create(baseUrl)
                .mutate()
                .defaultHeader("X-QR-API-KEY", apiKey)
                .build();
    }

    @Override
    public QrBadgeResponse generateQrBadge() {

        QrBadgeResponse response = executeQrRequest(
                () -> restClient.post()
                        .uri("/api/qr/generate-badge")
                        .retrieve()
                        .body(QrBadgeResponse.class),
                "generate QR badge"
        );

        if (response == null|| response.token() == null|| response.token().isBlank()
                || response.qrCode() == null
                || response.qrCode().isBlank()) {

            throw new ServiceUnavailableException(
                    "QR microservice returned an incomplete QR badge"
            );
        }

        System.out.println(response.token());
        log.info("QR_BADGE_GENERATION_SUCCEEDED");
        return response;

    }

    private <T> T executeQrRequest(Supplier<T> request,String operation) {

        try {
            return request.get();

        } catch (RestClientException ex) {

            log.error("QR_MICROSERVICE_REQUEST_FAILED: {}", operation, ex);

            throw new ServiceUnavailableException(
                    "QR service is currently unavailable. Please try again later.",
                    ex
            );
        }
    }
}
