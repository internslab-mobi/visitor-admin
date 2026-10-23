package com.qrservice.controller;

import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.dto.QrScanResponse;
import com.qrservice.service.QrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;

    @PostMapping("/generate-badge")
    public ResponseEntity<QrBadgeResponse> generateQrBadge(
            @Valid @RequestBody QrBadgeRequest request) {

        return ResponseEntity.ok(
                qrService.generateQrBadge(request)
        );
    }

    @PostMapping("/process-scan")
    public ResponseEntity<QrScanResponse> processScan(
            @RequestParam String token) {
        log.info(
                "QR scan input: length={}, startsWithTokenPrefix={}, containsEquals={}",
                token == null ? 0 : token.length(),
                token != null && token.startsWith("http"),
                token != null && token.contains("=")
        );

        return ResponseEntity.ok(
                qrService.processScan(token)
        );
    }


    @PostMapping("/invalidate-badge")
    public ResponseEntity<Void> invalidateBadgeOnCheckout(
            @RequestBody CheckoutBadgeInvalidationRequest request
    ) {
        qrService.invalidateBadgeOnCheckout(request);
        return ResponseEntity.noContent().build();
    }

}