package com.qrservice.controller;


import com.qrservice.dto.QrGenerateRequest;
import com.qrservice.dto.QrGenerateResponse;
import com.qrservice.service.QrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken() {

        String token = qrService.generateToken();

        return ResponseEntity.ok(
                Map.of("token", token)
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<QrGenerateResponse> generateQrCode(
            @Valid @RequestBody QrGenerateRequest request
    ) {

        String qrCode = qrService.generateQrCode(
                request.payload()
        );

        QrGenerateResponse response = new QrGenerateResponse(
                request.payload(),
                qrCode
        );

        return ResponseEntity.ok(response);
    }
}

