package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.NotificationRequest;

public interface NotificationOutboxService {

    void enqueue(NotificationRequest request);

    boolean claim(String id);

    void markSent(String id);

    void markFailed(String id, String error);

    void processPendingNotifications();
}