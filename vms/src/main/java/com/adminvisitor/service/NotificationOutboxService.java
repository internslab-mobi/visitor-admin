package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.NotificationRequest;

public interface NotificationOutboxService {

    void enqueue(NotificationRequest request);
}