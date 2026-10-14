package com.adminvisitor.auth.service.impl;

import com.adminvisitor.auth.entity.IdSequence;
import com.adminvisitor.auth.repository.IdSequenceRepository;
import com.adminvisitor.auth.service.IdGeneratorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdGeneratorServiceImpl implements IdGeneratorService {

    private final IdSequenceRepository idSequenceRepository;

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
