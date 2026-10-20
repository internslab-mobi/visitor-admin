
package com.adminvisitor.notification.dto.request;

import java.util.List;

public record EmailContent(
        String subject,

        // Header appearance — decided by Notification Service
        String heading,
        String subtitle,
        String headerColor,
        String headerIcon,
        String iconColor,

        // Main email content
        String greeting,
        String message,

        // Optional highlighted reference
        String highlightLabel,
        String highlightValue,
        String highlightCaption,

        // Reusable detail rows
        String detailsHeading,
        List<EmailDetail> details,

        // Repeated cards, such as daily expected visitors
        String itemsHeading,
        List<EmailItem> items,

        // Reusable notice section
        String noticeHeading,
        String noticeMessage,
        String noticeColor,
        String noticeBackground,

        // Closing text
        String closingMessage,

        // Optional inline images, such as badge QR and visitor photo
        List<InlineImage> inlineImages,

        // Shown when an optional image is unavailable
        String imagePlaceholder
) {
    public record EmailDetail(
            String label,
            String value
    ) {}

    public record EmailItem(
            String title,
            List<EmailDetail> details
    ) {}

    public record InlineImage(
            String contentId,
            String base64Data,
            String contentType,
            String label,
            String altText,
            Integer width,
            Integer height
    ) {}
}
