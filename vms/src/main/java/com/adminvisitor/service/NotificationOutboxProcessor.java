package com.adminvisitor.service;

public interface NotificationOutboxProcessor {

    boolean claim(String id);

    void markSent(String id);

    void markFailed(String id, String error);
}