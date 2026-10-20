package com.adminvisitor.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class NotificationEnvTest {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Test
    void shouldLoadMailConfiguration() {
        assertFalse(mailHost.isBlank(), "Mail host is missing");
        assertFalse(mailUsername.isBlank(), "Mail username is missing");
        assertFalse(mailPassword.isBlank(), "Mail password is missing");
    }
}