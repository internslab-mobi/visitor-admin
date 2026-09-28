package com.adminvisitor.service;

import com.adminvisitor.enums.ProofType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class HmacBlindIndexService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretKey;

    public HmacBlindIndexService(
            @Value("${vms.security.hmac.blind-index-key}")
            String secretKey) {

        this.secretKey =
                secretKey.getBytes(StandardCharsets.UTF_8);
    }

    public String generateBlindIndex(
            ProofType proofType,
            String proofNumber) {

        String normalizedProofNumber =
                normalizeProofNumber(
                        proofType,
                        proofNumber
                );

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec keySpec =
                    new SecretKeySpec(
                            secretKey,
                            HMAC_ALGORITHM
                    );

            mac.init(keySpec);

            byte[] hmacBytes =
                    mac.doFinal(
                            normalizedProofNumber
                                    .getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hmacBytes);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate HMAC blind index",
                    exception
            );
        }
    }

    private String normalizeProofNumber(
            ProofType proofType,
            String proofNumber) {

        if (proofNumber == null || proofNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Proof number cannot be empty"
            );
        }

        String normalized = proofNumber.trim();

        return switch (proofType) {

            case AADHAAR -> {
                String value =
                        normalized.replaceAll("\\s+", "");

                if (!value.matches("\\d{12}")) {
                    throw new IllegalArgumentException(
                            "Aadhaar must contain exactly 12 digits"
                    );
                }

                yield value;
            }

            case PAN -> {
                String value =
                        normalized.toUpperCase();

                if (!value.matches("[A-Z0-9]{10}")) {
                    throw new IllegalArgumentException(
                            "PAN must contain exactly 10 alphanumeric characters"
                    );
                }

                yield value;
            }

            case PASSPORT -> {
                String value =
                        normalized.replaceAll("\\s+", "");

                if (value.isBlank()) {
                    throw new IllegalArgumentException(
                            "Passport number cannot be empty"
                    );
                }

                yield value;
            }
        };
    }
}