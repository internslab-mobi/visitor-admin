package com.adminvisitor.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Represents a request to enqueue or send a notification.
 *
 * @param notificationId unique identifier used to identify the notification
 * @param scenario       notification scenario that determines the type of notification
 * @param recipient      email address of the primary recipient
 * @param cc             optional CC recipient email address
 * @param data           scenario-specific notification data
 */
@Schema(description = "Request containing the details required to send a notification")
public record NotificationRequest(

        @Schema(
                description = "Unique identifier for the notification",
                example = "NTF-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String notificationId,

        @Schema(
                description = "Scenario identifying the type of notification",
                example = "VISIT_CONFIRMATION",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String scenario,

        @Schema(
                description = "Email address of the primary recipient",
                example = "visitor@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String recipient,

        @Schema(
                description = "Email address of the CC recipient, if applicable",
                example = "host@example.com",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String cc,

        @Schema(
                description = "Map containing scenario-specific notification data",
                example = "{\"visitorName\":\"Arun Kumar\",\"visitReference\":\"VIS-001\"}",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Map<String, Object> data
) {
}