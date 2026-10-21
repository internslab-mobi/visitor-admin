package com.adminvisitor.dto.requestdto;

import java.util.Map;

public record NotificationRequest(
        String notificationId,
        String scenario,
        String recipient,
        String cc,
        Map<String, Object> data
) {
}