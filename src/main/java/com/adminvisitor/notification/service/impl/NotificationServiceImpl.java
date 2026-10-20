package com.adminvisitor.notification.service.impl;

import com.adminvisitor.notification.dto.request.EmailContent;
import com.adminvisitor.notification.dto.request.NotificationRequest;
import com.adminvisitor.notification.service.EmailContentBuilder;
import com.adminvisitor.notification.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailContentBuilder emailContentBuilder;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendNotification(NotificationRequest request) {

        EmailContent content = emailContentBuilder.build(request);

        Context context = new Context();

        context.setVariable("heading", content.heading());
        context.setVariable("subtitle", content.subtitle());
        context.setVariable("headerColor", content.headerColor());
        context.setVariable("headerIcon", content.headerIcon());
        context.setVariable("iconColor", content.iconColor());
        context.setVariable("greeting", content.greeting());
        context.setVariable("message", content.message());
        context.setVariable("highlightLabel", content.highlightLabel());
        context.setVariable("highlightValue", content.highlightValue());
        context.setVariable("highlightCaption", content.highlightCaption());
        context.setVariable("detailsHeading", content.detailsHeading());
        context.setVariable("details", content.details());
        context.setVariable("itemsHeading", content.itemsHeading());
        context.setVariable("items", content.items());
        context.setVariable("noticeHeading", content.noticeHeading());
        context.setVariable("noticeMessage", content.noticeMessage());
        context.setVariable("noticeColor", content.noticeColor());
        context.setVariable("noticeBackground", content.noticeBackground());
        context.setVariable("closingMessage", content.closingMessage());
        context.setVariable("imagePlaceholder", content.imagePlaceholder());

        context.setVariable("inlineImages", content.inlineImages());

        boolean hasVisitorPhoto = content.inlineImages() != null
                && content.inlineImages().stream()
                .anyMatch(image -> "visitorPhoto".equals(image.contentId()));

        context.setVariable("hasVisitorPhoto", hasVisitorPhoto);

        String htmlBody = templateEngine.process("generic-email", context);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(request.recipient());

            if (request.cc() != null && !request.cc().isBlank()) {
                helper.setCc(request.cc());
            }

            helper.setSubject(content.subject());
            helper.setText(htmlBody, true);

            if (content.inlineImages() != null) {
                for (EmailContent.InlineImage image : content.inlineImages()) {
                    byte[] imageBytes = java.util.Base64.getDecoder()
                            .decode(image.base64Data());

                    org.springframework.core.io.ByteArrayResource resource =
                            new org.springframework.core.io.ByteArrayResource(imageBytes);

                    helper.addInline(
                            image.contentId(),
                            resource,
                            image.contentType()
                    );
                }
            }

            mailSender.send(mimeMessage);

            log.info(
                    "Notification email sent. scenario={}",
                    request.scenario()
            );

        } catch (MessagingException | MailException exception) {
            log.error(
                    "Notification email delivery failed. scenario={}",
                    request.scenario(),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to send notification email",
                    exception
            );
        }
    }
}