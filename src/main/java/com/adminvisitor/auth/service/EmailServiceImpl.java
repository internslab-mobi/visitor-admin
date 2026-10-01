package com.adminvisitor.auth.service;

import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.exception.EmailSendException;
import org.springframework.mail.MailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    @Override
    public void sendOnboardingEmail(UserCreatedEvent data) {

        Context context = new Context();

        context.setVariable(
                "recipientName",
                data.recipientName()
        );

        context.setVariable(
                "recipientEmail",
                data.recipientEmail()
        );

        context.setVariable(
                "temporaryPassword",
                data.temporaryPassword()
        );

        String htmlContent =
                templateEngine.process(
                        "email/onboarding-email",
                        context
                );

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setTo(data.recipientEmail());

            helper.setSubject(
                    "Welcome to Admin Management System - Your Account Credentials"
            );

            helper.setText(
                    htmlContent,
                    true
            );

            mailSender.send(message);

            log.info(
                    "Onboarding email sent successfully. recipient={}",
                    data.recipientEmail()
            );

        } catch (MessagingException | MailException exception) {

            log.error(
                    "Failed to send onboarding email. recipient={}",
                    data.recipientEmail(),
                    exception
            );

            throw new EmailSendException(
                    "Failed to send onboarding email to "
                            + data.recipientEmail(),
                    exception
            );
        }
    }
}