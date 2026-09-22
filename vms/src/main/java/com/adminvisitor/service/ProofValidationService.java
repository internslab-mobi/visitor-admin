package com.adminvisitor.service;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import org.springframework.stereotype.Service;

@Service
public class ProofValidationService {

    public void validate(
            Nationality nationality,
            ProofType proofType) {

        if (nationality == null) {
            throw new IllegalArgumentException(
                    "Nationality is required"
            );
        }

        if (proofType == null) {
            throw new IllegalArgumentException(
                    "Proof type is required"
            );
        }

        boolean valid = switch (nationality) {

            case DOMESTIC ->
                    proofType == ProofType.AADHAAR
                            || proofType == ProofType.PAN;

            case INTERNATIONAL ->
                    proofType == ProofType.PASSPORT;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Proof type "
                            + proofType
                            + " is not valid for nationality "
                            + nationality
            );
        }
    }
}