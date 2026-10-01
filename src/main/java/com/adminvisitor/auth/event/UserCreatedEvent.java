package com.adminvisitor.auth.event;

public record UserCreatedEvent(
        String recipientEmail,
        String recipientName,
        String temporaryPassword
) {
}