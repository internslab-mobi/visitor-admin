package com.adminvisitor.service;

import com.adminvisitor.entity.IdSequence;
import com.adminvisitor.repository.IdSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdGeneratorService {

    private final IdSequenceRepository idSequenceRepository;

    @Transactional
    public String generateId(String sequenceName, String prefix) {

        IdSequence sequence =
                idSequenceRepository.findBySequenceNameForUpdate(sequenceName)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "ID sequence not configured: " + sequenceName
                                )
                        );

        Long currentValue = sequence.getNextValue();

        sequence.setNextValue(currentValue + 1);

        idSequenceRepository.save(sequence);

        return String.format("%s-%03d", prefix, currentValue);
    }

}