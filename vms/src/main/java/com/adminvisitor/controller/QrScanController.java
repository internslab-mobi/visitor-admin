package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.QrScanResponse;
import com.adminvisitor.service.QrScanService;
import com.adminvisitor.service.VisitBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr-scan")
@RequiredArgsConstructor
public class QrScanController {

    //private final QrScanService qrScanService;
private final VisitBadgeService visitBadgeService;
    @PostMapping
    public ResponseEntity<QrScanResponse> processScan(
            @RequestParam String token) {

        QrScanResponse response =visitBadgeService.processScan(token);
               // qrScanService.processScan(token);

        return ResponseEntity.ok(response);
    }
}