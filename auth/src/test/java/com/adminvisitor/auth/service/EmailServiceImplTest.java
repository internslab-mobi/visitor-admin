package com.adminvisitor.auth.service;

import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.exception.EmailSendException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(
                mailSender,
                templateEngine
        );
    }

    @Test
    void testSendOnboardingEmailSuccess() {

        Session session =
                Session.getInstance(new Properties());

        MimeMessage mimeMessage =
                new MimeMessage(session);

        UserCreatedEvent data =
                new UserCreatedEvent(
                        "john@example.com",
                        "John",
                        "TempPassword123!"
                );

        when(templateEngine.process(
                eq("email/onboarding-email"),
                any()
        )).thenReturn(
                "<html><body>Test Email</body></html>"
        );

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendOnboardingEmail(data)
        );

        verify(templateEngine, times(1)).process(
                eq("email/onboarding-email"),
                any()
        );

        verify(mailSender, times(1))
                .send(mimeMessage);
    }

    @Test
    void testSendOnboardingEmailFailureThrowsEmailSendException() {

        Session session =
                Session.getInstance(new Properties());

        MimeMessage mimeMessage =
                new MimeMessage(session);

        UserCreatedEvent data =
                new UserCreatedEvent(
                        "john@example.com",
                        "John",
                        "TempPassword123!"
                );

        when(templateEngine.process(
                eq("email/onboarding-email"),
                any()
        )).thenReturn(
                "<html><body>Test Email</body></html>"
        );

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        doThrow(
                new MailSendException(
                        "SMTP server connection failed"
                )
        )
                .when(mailSender)
                .send(mimeMessage);

        assertThrows(
                EmailSendException.class,
                () -> emailService.sendOnboardingEmail(data)
        );

        verify(templateEngine, times(1)).process(
                eq("email/onboarding-email"),
                any()
        );

        verify(mailSender, times(1))
                .send(mimeMessage);
    }
}