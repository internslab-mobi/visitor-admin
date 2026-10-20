package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
class NotificationClientIntegrationTest {

    @Autowired
    private NotificationClientService notificationClient;

    private String testRecipient = "nethra161102@gmail.com";

    @Test
    void shouldSendEmailThroughNotificationService() {
        NotificationRequest request = new NotificationRequest(
                "VISIT_CONFIRMATION",
                testRecipient,
                null,
                Map.of(
                        "visitorName", "Integration Test Visitor",
                        "visitReference", "TEST-001",
                        "purpose", "Notification integration test",
                        "expectedArrival", "15 Oct 2026, 03:00 PM",
                        "expectedDeparture", "15 Oct 2026, 04:00 PM",
                        "hostName", "Test Host",
                        "departmentName", "Engineering"
                )
        );

        notificationClient.sendEmail(request);
    }
}