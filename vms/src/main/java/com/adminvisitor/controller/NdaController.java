package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.service.NdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/nda")
@RequiredArgsConstructor
public class NdaController {

    private final NdaService ndaLifecycleService;

    // ============================================================
    // CASE 1: Upload New NDA (Expired NDA or First NDA)
    // ============================================================

    @PostMapping(
            value = "/{visitorId}/upload-new",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> uploadNewNda(
            @PathVariable String visitorId,
            @RequestParam("ndaFile") MultipartFile ndaFile,
            @RequestParam("validUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate validUntil
    ) {
        DocumentMetadata metadata = ndaLifecycleService.uploadNewNda(
                visitorId,
                ndaFile,

                validUntil
        );

        DocumentResponse response = new DocumentResponse(
                metadata.getId(),
                metadata.getDocumentPath(),
                metadata.getCreatedAt() != null ? metadata.getCreatedAt().toString() : null,
                metadata.getValidUntil() != null ? metadata.getValidUntil().toString() : null
        );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // CASE 2: Extend Valid NDA (Requires Supporting Document)
    // ============================================================

    @PostMapping(
            value = "/{visitorId}/extend",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> extendNdaValidity(
            @PathVariable String visitorId,
            @RequestParam("supportingDocument") MultipartFile supportingDocument,
             @RequestParam("newValidUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate newValidUntil
    ) {
        DocumentMetadata metadata = ndaLifecycleService.extendNdaValidity(
                visitorId,
                supportingDocument,

                newValidUntil
        );

        DocumentResponse response = new DocumentResponse(
                metadata.getId(),
                metadata.getDocumentPath(),
                metadata.getCreatedAt() != null ? metadata.getCreatedAt().toString() : null,
                metadata.getValidUntil() != null ? metadata.getValidUntil().toString() : null
        );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Get Latest NDA
    // ============================================================

    @GetMapping("/{visitorId}/latest")
    public ResponseEntity<DocumentResponse> getLatestNda(
            @PathVariable String visitorId
    ) {
        DocumentMetadata metadata = ndaLifecycleService.getLatestNda(visitorId);

        DocumentResponse response = new DocumentResponse(
                metadata.getId(),
                metadata.getDocumentPath(),
                metadata.getCreatedAt() != null ? metadata.getCreatedAt().toString() : null,
                metadata.getValidUntil() != null ? metadata.getValidUntil().toString() : null
        );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // Get NDA History
    // ============================================================

    @GetMapping("/{visitorId}/history")
    public ResponseEntity<java.util.List<DocumentResponse>> getNdaHistory(
            @PathVariable String visitorId
    ) {
        return ResponseEntity.ok(ndaLifecycleService.getNdaHistory(visitorId));
    }
}
