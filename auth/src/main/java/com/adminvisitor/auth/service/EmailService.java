package com.adminvisitor.auth.service;

import com.adminvisitor.auth.event.UserCreatedEvent;

public interface EmailService {

    void sendOnboardingEmail(UserCreatedEvent data);
}
