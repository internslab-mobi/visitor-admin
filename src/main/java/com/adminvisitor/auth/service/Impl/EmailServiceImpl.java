
package com.adminvisitor.auth.service.Impl;

import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.exception.EmailSendException;
import com.adminvisitor.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@adminvisitor.com}")
    private String mailUsername;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            ITemplateEngine templateEngine
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    @Override
    public void sendOnboardingEmail(UserCreatedEvent data) {

        try {
            Context context = new Context();

            context.setVariable("recipientName", data.recipientName());
            context.setVariable("recipientEmail", data.recipientEmail());
            context.setVariable("temporaryPassword", data.temporaryPassword());

            String htmlContent = templateEngine.process(
                    "email/onboarding-email",
                    context
            );

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setTo(data.recipientEmail());
            helper.setFrom(mailUsername);
            helper.setSubject(
                    "Welcome to Admin Management System - Your Account Credentials"
            );
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("ONBOARDING_EMAIL_SENT");

        } catch (MessagingException | MailException exception) {

            log.error("ONBOARDING_EMAIL_FAILED", exception);

            throw new EmailSendException(
                    "Failed to send onboarding email",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendPasswordResetOtpEmail(
            String recipientEmail,
            String recipientName,
            String otp,
            int expirationMinutes
    ) {

        try {
            Context context = new Context();

            context.setVariable("recipientName", recipientName);
            context.setVariable("otp", otp);
            context.setVariable("expirationMinutes", expirationMinutes);

            String htmlContent = templateEngine.process(
                    "email/password-reset-email",
                    context
            );

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setTo(recipientEmail);
            helper.setFrom(mailUsername);
            helper.setSubject(
                    "Admin Management System - Password Reset Verification Code"
            );
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("PASSWORD_RESET_OTP_EMAIL_SENT");

        } catch (MessagingException | MailException exception) {

            log.error("PASSWORD_RESET_OTP_EMAIL_FAILED", exception);

            throw new EmailSendException(
                    "Failed to send verification code email",
                    exception
            );
        }
    }
}
