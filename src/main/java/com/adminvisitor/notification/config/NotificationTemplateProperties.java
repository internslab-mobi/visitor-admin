
package com.adminvisitor.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "notification.templates")
public record NotificationTemplateProperties(
        Map<String, ScenarioTemplate> scenarios
) {

    public record ScenarioTemplate(
            String subject,
            String heading,
            String subtitle,
            String headerColor,
            String headerIcon,
            String iconColor,
            String message,
            String highlightLabel,
            String highlightCaption,
            String detailsHeading,
            String itemsHeading,
            String noticeHeading,
            String noticeMessage,
            String noticeColor,
            String noticeBackground,
            String closingMessage,
            String imagePlaceholder,

            // New YAML-driven properties
            String greetingType,
            List<FieldMapping> details,
            List<FieldMapping> itemDetails
    ) {}

    public record FieldMapping(
            String field,
            String label
    ) {}
}
