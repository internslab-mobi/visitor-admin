package com.adminvisitor.service.impl;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.service.ProofSecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Service
public class ProofSecurityServiceImpl implements ProofSecurityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretKey;

    public ProofSecurityServiceImpl(
            @Value("${vms.security.hmac.blind-index-key}") String secretKey
    ) {
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    public String generateBlindIndex(ProofType proofType, String proofNumber) {

        String normalizedProofNumber =
                normalizeProofNumber(proofType, proofNumber);

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey,
                    HMAC_ALGORITHM
            );

            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(
                    normalizedProofNumber.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hmacBytes);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Failed to generate HMAC blind index",
                    exception
            );
        }
    }

    private String normalizeProofNumber(
            ProofType proofType,
            String proofNumber
    ) {
        if (proofNumber == null || proofNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Proof number cannot be empty"
            );
        }

        String normalized = proofNumber.trim();

        return switch (proofType) {

            case AADHAAR -> {
                String value = normalized.replaceAll("\\s+", "");

                if (!value.matches("\\d{12}")) {
                    throw new IllegalArgumentException(
                            "Aadhaar must contain exactly 12 digits"
                    );
                }

                yield value;
            }

            case PAN -> {
                String value = normalized.toUpperCase();

                if (!value.matches("[A-Z0-9]{10}")) {
                    throw new IllegalArgumentException(
                            "PAN must contain exactly 10 alphanumeric characters"
                    );
                }

                yield value;
            }

            case PASSPORT -> {
                String value = normalized.replaceAll("\\s+", "");

                if (value.isBlank()) {
                    throw new IllegalArgumentException(
                            "Passport number cannot be empty"
                    );
                }

                yield value;
            }
        };
    }

    public void validate(
            Nationality nationality,
            ProofType proofType
    ) {
        if (nationality == null) {
            throw new IllegalArgumentException("Nationality is required");
        }

        if (proofType == null) {
            throw new IllegalArgumentException("Proof type is required");
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
                    "Proof type " + proofType
                            + " is not valid for nationality " + nationality
            );
        }
    }

    public void validate(
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    ) {
        if (nationality == null) {
            throw new IllegalArgumentException("Nationality is required");
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

                if (passportNumber != null && !passportNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Passport is not allowed for domestic visitors"
                    );
                }
            }

            case INTERNATIONAL -> {
                if (passportNumber == null || passportNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Passport number is required for international visitors"
                    );
                }

                if (aadharNumber != null && !aadharNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "Aadhaar is not allowed for international visitors"
                    );
                }

                if (panNumber != null && !panNumber.isBlank()) {
                    throw new IllegalArgumentException(
                            "PAN is not allowed for international visitors"
                    );
                }
            }
        }
    }
}