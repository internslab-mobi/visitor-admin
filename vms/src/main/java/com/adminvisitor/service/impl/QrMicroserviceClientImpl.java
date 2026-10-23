
package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.CheckoutBadgeInvalidationRequest;
import com.adminvisitor.dto.requestdto.QrBadgeRequest;
import com.adminvisitor.dto.responsedto.QrBadgeResponse;
import com.adminvisitor.exception.ServiceUnavailableException;
import com.adminvisitor.service.QrMicroserviceClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;
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



    public QrBadgeResponse generateQrBadge(QrBadgeRequest request) {
        return executeQrRequest(
                () -> restClient.post()
                        .uri("/api/qr/generate-badge")
                        .body(request)
                        .retrieve()
                        .body(QrBadgeResponse.class),
                "generate QR badge"
        );
    }


    @Override
    public void invalidateBadgeOnCheckout(
            String visitId,
            LocalDateTime checkoutTime
    ) {
        executeQrRequest(
                () -> {
                    restClient.post()
                            .uri("/api/qr/invalidate-badge")
                            .body(Map.of(
                                    "visitId", visitId,
                                    "checkoutTime", checkoutTime
                            ))
                            .retrieve()
                            .toBodilessEntity();

                    return null;
                },
                "invalidate QR badge on checkout"
        );
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
