package com.adminvisitor.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {

        try {
            byte[] randomBytes = new byte[32];

          //  Fills the 32-byte array with cryptographically secure random data.
          //  Before this line: randomBytes = empty/uninitialized byte values
            // After this line: randomBytes = random unpredictable bytes
            secureRandom.nextBytes(randomBytes);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(randomBytes);

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate QR token",
                    e
            );
        }
    }


    public String generateQrCode(String qrToken) {

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();

            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix =
                    new MultiFormatWriter().encode(
                            qrToken,
                            BarcodeFormat.QR_CODE,
                            300,
                            300,
                            hints
                    );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return java.util.Base64
                    .getEncoder()
                    .encodeToString(
                            outputStream.toByteArray()
                    );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate QR code",
                    e
            );
        }
    }
}