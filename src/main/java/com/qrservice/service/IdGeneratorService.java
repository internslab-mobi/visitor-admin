package com.qrservice.service;

public interface IdGeneratorService {
    String generateId(String sequenceName, String prefix);
}