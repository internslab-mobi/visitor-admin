
package com.adminvisitor.notification.service.impl;

import com.adminvisitor.notification.config.NotificationTemplateProperties;
import com.adminvisitor.notification.config.NotificationTemplateProperties.FieldMapping;
import com.adminvisitor.notification.config.NotificationTemplateProperties.ScenarioTemplate;
import com.adminvisitor.notification.dto.request.EmailContent;
import com.adminvisitor.notification.dto.request.NotificationRequest;
import com.adminvisitor.notification.service.EmailContentBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailContentBuilderImpl implements EmailContentBuilder {

    private final NotificationTemplateProperties templateProperties;

    @Override
    public EmailContent build(NotificationRequest request) {

        Map<String, Object> data = request.data() == null
                ? Map.of()
                : request.data();

        ScenarioTemplate template = templateProperties.scenarios()
                .get(request.scenario().name());

        if (template == null) {
            throw new IllegalArgumentException(
                    "No email template configuration found for scenario: "
                            + request.scenario()
            );
        }

        String reference = value(data, "visitReference");

        String subject = template.subject()
                .replace("{visitReference}", reference)
                .replace("{reminderDate}", value(data, "reminderDate"));

        String greeting = buildGreeting(template.greetingType(), data);

        List<EmailContent.EmailDetail> details =
                buildDetails(template.details(), data);

        List<EmailContent.EmailItem> items =
                buildItems(template.itemDetails(), data);

        String highlightValue =
                template.highlightLabel() == null ? null : reference;

        return new EmailContent(
                subject,
                template.heading(),
                template.subtitle() == null
                        ? null
                        : template.subtitle()
                          .replace("{reminderDate}", value(data, "reminderDate")),
                template.headerColor(),
                template.headerIcon(),
                template.iconColor(),
                greeting,
                template.message(),
                template.highlightLabel(),
                highlightValue,
                template.highlightCaption(),
                template.detailsHeading(),
                details,
                template.itemsHeading(),
                items,
                template.noticeHeading(),
                template.noticeMessage(),
                template.noticeColor(),
                template.noticeBackground(),
                template.closingMessage(),
                buildBadgeImages(request.scenario().name(), data),
                template.imagePlaceholder()
        );
    }

    private String buildGreeting(
            String greetingType,
            Map<String, Object> data) {

        if (greetingType == null || greetingType.isBlank()) {
            return "";
        }

        String name = switch (greetingType.toLowerCase()) {
            case "visitor" -> value(data, "visitorName");
            case "host" -> value(data, "hostName");
            default -> throw new IllegalArgumentException(
                    "Unsupported greeting type: " + greetingType
            );
        };

        return "Dear " + name + ",";
    }

    private List<EmailContent.EmailDetail> buildDetails(
            List<FieldMapping> mappings,
            Map<String, Object> data) {

        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        List<EmailContent.EmailDetail> result = new ArrayList<>();

        for (FieldMapping mapping : mappings) {
            String fieldValue = value(data, mapping.field());

            if (!fieldValue.isBlank()) {
                result.add(new EmailContent.EmailDetail(
                        mapping.label(),
                        fieldValue
                ));
            }
        }

        return result;
    }

    private List<EmailContent.EmailItem> buildItems(
            List<FieldMapping> mappings,
            Map<String, Object> data) {

        Object rawVisits = data.get("visits");

        if (!(rawVisits instanceof List<?> visits)
                || visits.isEmpty()
                || mappings == null
                || mappings.isEmpty()) {
            return List.of();
        }

        List<EmailContent.EmailItem> result = new ArrayList<>();

        for (Object rawVisit : visits) {
            if (!(rawVisit instanceof Map<?, ?> visit)) {
                continue;
            }

            Map<String, Object> visitData = new HashMap<>();

            visit.forEach((key, value) -> {
                if (key instanceof String stringKey && value != null) {
                    visitData.put(stringKey, value);
                }
            });

            String visitorName = value(visitData, "visitorName");

            result.add(new EmailContent.EmailItem(
                    visitorName.isBlank()
                            ? "Expected Visitor"
                            : visitorName,
                    buildDetails(mappings, visitData)
            ));
        }

        return result;
    }

    private List<EmailContent.InlineImage> buildBadgeImages(
            String scenario,
            Map<String, Object> data) {

        if (!"VISIT_BADGE".equals(scenario)) {
            return List.of();
        }

        List<EmailContent.InlineImage> images = new ArrayList<>();

        addImage(images, data, "qrCode", "qrCode",
                "image/png", "Visitor QR Code", "Visitor badge QR code");

        addImage(images, data, "visitorPhotoBase64", "visitorPhoto",
                "image/jpeg", "Visitor Photo", "Visitor photograph");

        return images;
    }

    private void addImage(
            List<EmailContent.InlineImage> images,
            Map<String, Object> data,
            String dataKey,
            String contentId,
            String contentType,
            String label,
            String altText) {

        String base64 = value(data, dataKey);

        if (!base64.isBlank()) {
            images.add(new EmailContent.InlineImage(
                    contentId,
                    base64,
                    contentType,
                    label,
                    altText,
                    300,
                    300
            ));
        }
    }

    private String value(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
