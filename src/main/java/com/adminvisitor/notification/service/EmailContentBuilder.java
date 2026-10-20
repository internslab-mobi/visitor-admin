package com.adminvisitor.notification.service;

import com.adminvisitor.notification.dto.request.EmailContent;
import com.adminvisitor.notification.dto.request.NotificationRequest;

public interface EmailContentBuilder {

    EmailContent build(NotificationRequest request);
}