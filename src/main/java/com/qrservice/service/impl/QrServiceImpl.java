package com.qrservice.service.impl;


import com.qrservice.service.QrService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrServiceImpl implements QrService {

    private final SecureRandom secureRandom = new SecureRandom();
    @Override
    public String generateQrCode(String payload) {

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "QR payload must not be empty"
            );
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    300,
                    300,
                    hints
            );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return Base64.getEncoder()
                    .encodeToString(outputStream.toByteArray());

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate QR code",
                    exception
            );
        }
    }

    @Override
    public String generateToken() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

}
