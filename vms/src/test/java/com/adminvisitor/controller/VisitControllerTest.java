package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.service.VisitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitControllerTest {

    @Mock
    private VisitService visitService;

    @InjectMocks
    private VisitController visitController;

    @Test
    void register_shouldReturnCreatedResponse() {

        RegistrationRequest request = new RegistrationRequest(
                RegistrationType.PRE_REGISTRATION,
                VisitorType.GUEST,
                "John",
                "Doe",
                "john.doe@example.com",
                "9876543210",
                "ABC Technologies",
                "Business meeting",
                1L,
                LocalDate.of(2026, 9, 20),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "First-time visitor",
                null,
                null
        );

        RegistrationResponse expectedResponse = new RegistrationResponse(
                "VT-001",
                "VIS-20260920100000-ABC12345",
                "VS-001",
                "John",
                "Doe",
                "john.doe@example.com",
                "9876543210",
                "ABC Technologies",
                VisitorType.GUEST,
                RegistrationType.PRE_REGISTRATION,
                "Business meeting",
                1L,
                1L,
                LocalDateTime.of(2026, 9, 20, 10, 0),
                LocalDateTime.of(2026, 9, 20, 11, 0),
                "First-time visitor",
                VisitStatus.REGISTERED,
                "Visitor pre-registered successfully"
        );

        when(visitService.register(request))
                .thenReturn(expectedResponse);

        ResponseEntity<RegistrationResponse> actualResponse =
                visitController.register(request);

        assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
        assertSame(expectedResponse, actualResponse.getBody());

        verify(visitService).register(request);
        verifyNoMoreInteractions(visitService);
    }
}