package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.NotificationRequest;

public interface NotificationClientService {

    void sendEmail(NotificationRequest request);
}