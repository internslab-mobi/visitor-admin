package com.adminvisitor.notification.dto.request;

import com.adminvisitor.notification.enums.NotificationScenario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NotificationRequest(

        @NotBlank
        String notificationId,
        @NotNull
        NotificationScenario scenario,
        @NotBlank
        @Email
        String recipient,
        @Email
        String cc,
        Map<String, Object> data
) {
}