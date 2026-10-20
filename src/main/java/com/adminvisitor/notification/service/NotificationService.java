package com.adminvisitor.notification.service;

import com.adminvisitor.notification.dto.request.NotificationRequest;

public interface NotificationService {

    void sendNotification(NotificationRequest request);
}