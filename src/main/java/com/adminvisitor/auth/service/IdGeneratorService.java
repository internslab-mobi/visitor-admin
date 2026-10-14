package com.adminvisitor.auth.service;

public interface IdGeneratorService {

    String generateId(String sequenceName, String prefix);
}
