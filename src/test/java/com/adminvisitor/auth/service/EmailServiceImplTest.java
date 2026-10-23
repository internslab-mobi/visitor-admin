package com.adminvisitor.auth.service;

import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.service.Impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ITemplateEngine templateEngine;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(
                mailSender,
                templateEngine
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                emailService,
                "mailUsername",
                "noreply@adminvisitor.com"
        );
    }

    @Test
    void testSendOnboardingEmailSuccess() {

        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);

        UserCreatedEvent data = new UserCreatedEvent(
                "john@example.com",
                "John",
                "TempPassword123!"
        );

        when(templateEngine.process(
                eq("email/onboarding-email"),
                any(IContext.class)
        )).thenReturn("<html><body>Test Email</body></html>");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendOnboardingEmail(data));

        verify(templateEngine, times(1)).process(
                eq("email/onboarding-email"),
                any(IContext.class)
        );

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetOtpEmailSuccess() {
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);

        when(templateEngine.process(
                eq("email/password-reset-email"),
                any(IContext.class)
        )).thenReturn("<html><body>OTP Email</body></html>");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendPasswordResetOtpEmail(
                "john@example.com",
                "Logeshwaran J",
                "525520",
                5
        ));

        verify(templateEngine, times(1)).process(
                eq("email/password-reset-email"),
                any(IContext.class)
        );

        verify(mailSender, times(1)).send(mimeMessage);
    }
}