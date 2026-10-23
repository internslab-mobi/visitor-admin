package com.adminvisitor.auth.service;

/**
 * Defines the contract for generating unique identifiers using named sequences
 * and specified prefixes.
 *
 * @author Logeshwaran
 */
public interface IdGeneratorService {

    /**
     * Generates a unique identifier using the specified sequence and prefix.
     *
     * @param sequenceName the name of the sequence used to generate the identifier
     * @param prefix the prefix to prepend to the generated identifier
     * @return the generated identifier as a string
     */
    String generateId(String sequenceName, String prefix);
}