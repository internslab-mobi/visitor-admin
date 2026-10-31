package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.service.impl.NotificationClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NotificationClientServiceImplTest {

    private RestClient notificationRestClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private NotificationClientServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationRestClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        service = new NotificationClientServiceImpl(
                notificationRestClient
        );
    }

    @Test
    void sendEmail_shouldSendRequestSuccessfully() {
        NotificationRequest request = mock(NotificationRequest.class);
        when(request.scenario()).thenReturn(null);

        when(notificationRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/notifications/email"))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(request))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.toBodilessEntity()).thenReturn(null);

        service.sendEmail(request);

        verify(notificationRestClient).post();
        verify(requestBodyUriSpec).uri("/api/notifications/email");
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec).body(request);
        verify(requestBodySpec).retrieve();
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void sendEmail_shouldPropagateExceptionWhenNotificationServiceFails() {
        NotificationRequest request = mock(NotificationRequest.class);

        when(notificationRestClient.post())
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> service.sendEmail(request)
        );
    }
}