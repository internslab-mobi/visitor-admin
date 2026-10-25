
package com.adminvisitor.service.impl;


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

/**
 * Service implementation for communicating with the QR microservice.
 *
 * <p>Provides operations for generating QR badges and invalidating
 * badges when a visitor checks out. Uses Spring's {@link RestClient}
 * to send HTTP requests to the configured QR microservice.</p>
 *
 * <p>Includes API key authentication for outgoing requests and
 * centralizes REST client exception handling to report when the
 * QR microservice is unavailable.</p>
 */
@Service
@Slf4j
public class QrMicroserviceClientImpl implements QrMicroserviceClient {

    private final RestClient restClient;


    /**
     * Initializes the QR microservice client.
     *
     * <p>Configures the base URL and adds the QR service API key
     * to outgoing requests using the {@code X-QR-API-KEY} header.</p>
     *
     * @param baseUrl the configured base URL of the QR microservice
     * @param apiKey the API key used to authenticate requests
     * @throws IllegalStateException if the API key is null or blank
     */
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


    /**
     * Requests the QR microservice to generate a QR badge.
     *
     * <p>Sends the badge request to the QR microservice and returns
     * the response containing the generated badge details.</p>
     *
     * @param request the details required to generate the QR badge
     * @return the response containing the generated QR badge details
     * @throws ServiceUnavailableException if the QR microservice request fails
     */

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

    /**
     * Requests the QR microservice to invalidate a visitor's badge
     * when checkout occurs.
     *
     * <p>Sends the visit identifier and checkout timestamp to the
     * QR microservice for badge invalidation.</p>
     *
     * @param visitId the unique identifier of the visit
     * @param checkoutTime the date and time when the visitor checked out
     * @throws ServiceUnavailableException if the QR microservice request fails
     */
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

    /**
     * Executes a QR microservice request and handles REST client failures.
     *
     * <p>Logs the failed operation and wraps any {@link RestClientException}
     * in a {@link ServiceUnavailableException} so callers receive a
     * consistent service-unavailable error.</p>
     *
     * @param <T> the type of the request result
     * @param request the operation to execute
     * @param operation a description of the operation for logging
     * @return the result returned by the request operation, or {@code null}
     *         when the operation does not return a value
     * @throws ServiceUnavailableException if a REST client exception occurs
     */
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
