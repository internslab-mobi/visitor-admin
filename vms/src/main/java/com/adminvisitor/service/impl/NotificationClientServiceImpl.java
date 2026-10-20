package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClientServiceImpl implements NotificationClientService {

    private final RestClient.Builder restClientBuilder;

    @Value("${notification.service.base-url}")
    private String notificationServiceUrl;

    @Override
    public void sendEmail(NotificationRequest request) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(60));

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
                "NOTIFICATION_REQUEST_ACCEPTED scenario={}",
                request.scenario()
        );
    }
}