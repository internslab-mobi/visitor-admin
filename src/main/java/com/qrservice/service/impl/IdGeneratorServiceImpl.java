package com.qrservice.service.impl;

import com.qrservice.entity.IdSequence;
import com.qrservice.repository.IdSequenceRepository;
import com.qrservice.service.IdGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdGeneratorServiceImpl implements IdGeneratorService {

    private final IdSequenceRepository idSequenceRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String generateId(String sequenceName, String prefix) {

        IdSequence sequence = idSequenceRepository
                .findBySequenceNameForUpdate(sequenceName)
                .orElseThrow(() -> new IllegalStateException(
                        "ID sequence not configured: " + sequenceName
                ));

        long currentValue = sequence.getNextValue();
        sequence.setNextValue(currentValue + 1);

        idSequenceRepository.save(sequence);

        return String.format("%s-%03d", prefix, currentValue);
    }
}