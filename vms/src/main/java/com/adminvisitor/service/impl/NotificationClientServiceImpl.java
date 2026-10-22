
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
public class NotificationClientServiceImpl
        implements NotificationClientService {

    private final RestClient notificationRestClient;

    @Value("${notification.service.base-url}")
    private String notificationServiceUrl;

    @Override
    public void sendEmail(NotificationRequest request) {

        notificationRestClient.post()
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
