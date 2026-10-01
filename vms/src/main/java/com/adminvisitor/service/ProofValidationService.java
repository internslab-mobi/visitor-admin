package com.adminvisitor.service;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import org.springframework.stereotype.Service;

@Service
public class ProofValidationService {

    public void validate(
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    ) {

        if (nationality == null) {
            throw new IllegalArgumentException(
                    "Nationality is required"
            );
        }

        switch (nationality) {

            case DOMESTIC -> {

                if (aadharNumber == null || aadharNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Aadhaar number is required for domestic visitors"
                    );
                }

                if (panNumber == null || panNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "PAN number is required for domestic visitors"
                    );
                }

                if (passportNumber != null
                        && !passportNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Passport is not allowed for domestic visitors"
                    );
                }
            }

            case INTERNATIONAL -> {

                if (passportNumber == null
                        || passportNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Passport number is required for international visitors"
                    );
                }

                if (aadharNumber != null
                        && !aadharNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Aadhaar is not allowed for international visitors"
                    );
                }

                if (panNumber != null
                        && !panNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "PAN is not allowed for international visitors"
                    );
                }
            }
        }
    }



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