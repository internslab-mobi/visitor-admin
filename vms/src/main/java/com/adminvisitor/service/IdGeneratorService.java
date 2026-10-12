package com.adminvisitor.service;

public interface IdGeneratorService {

    String generateId(String sequenceName, String prefix);
}