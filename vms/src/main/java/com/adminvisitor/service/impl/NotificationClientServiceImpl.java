package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClientServiceImpl implements NotificationClientService {

    private final RestClient.Builder restClientBuilder;

    @Value("${notification.service.base-url}")
    private String notificationServiceUrl;


    @Override
    public void sendEmail(NotificationRequest request) {
        java.net.http.HttpClient httpClient =
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(5))
                        .build();

        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(60));

        RestClient client = restClientBuilder
                .baseUrl(notificationServiceUrl)
                .requestFactory(requestFactory)
                .build();

        client.post()
                .uri("/api/notifications/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info(
                "Notification request accepted. scenario={}",
                request.scenario()
        );
    }

}
