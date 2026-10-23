package com.adminvisitor.auth.service.Impl;

import com.adminvisitor.auth.entity.IdSequence;
import com.adminvisitor.auth.repository.IdSequenceRepository;
import com.adminvisitor.auth.service.IdGeneratorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements identifier generation using database-managed sequences.
 * Retrieves and updates sequence values within a transaction to maintain
 * consistency during concurrent identifier generation.
 *
 * @author Logeshwaran
 */
@Service
@RequiredArgsConstructor
public class IdGeneratorServiceImpl implements IdGeneratorService {

    private final IdSequenceRepository idSequenceRepository;

    /**
     * Generates an identifier using the specified sequence and prefix.
     * Locks the sequence record, retrieves its current value, increments
     * the stored value, and formats the identifier with a three-digit number.
     *
     * @param sequenceName the name of the sequence used to generate the identifier
     * @param prefix the prefix to prepend to the generated identifier
     * @return the generated identifier containing the prefix and sequence value
     * @throws EntityNotFoundException if the specified sequence is not configured
     */
    @Override
    @Transactional
    public String generateId(String sequenceName, String prefix) {

        IdSequence sequence = idSequenceRepository
                .findBySequenceNameForUpdate(sequenceName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ID sequence not configured: " + sequenceName
                ));

        long currentValue = sequence.getNextValue();

        sequence.setNextValue(currentValue + 1);

        return String.format("%s%03d", prefix, currentValue);
    }
}