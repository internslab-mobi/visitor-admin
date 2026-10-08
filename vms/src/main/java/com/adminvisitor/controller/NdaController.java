package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.NdaExtensionResult;
import com.adminvisitor.dto.responsedto.NdaLatestResult;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.service.NdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

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

                    // Actual upload/creation time
                    metadata.getCreatedAt() != null
                            ? metadata.getCreatedAt().toString()
                            : null,

                    // New NDA: Valid From = Created At
                    metadata.getCreatedAt() != null
                            ? metadata.getCreatedAt().toString()
                            : null,

                    // Valid Until
                    metadata.getValidUntil() != null
                            ? metadata.getValidUntil().toString()
                            : null,

                    // New NDA is not overriding another NDA
                    null
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
            @RequestParam("newValidUntil")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate newValidUntil
    ) {

        NdaExtensionResult result =
                ndaLifecycleService.extendNdaValidity(
                        visitorId,
                        supportingDocument,
                        newValidUntil
                );

        DocumentMetadata metadata = result.metadata();

        DocumentResponse response = new DocumentResponse(
                metadata.getId(),
                metadata.getDocumentPath(),

                // Actual upload/creation time of supporting document
                metadata.getCreatedAt() != null
                        ? metadata.getCreatedAt().toString()
                        : null,

                // Valid From = original NDA's createdAt
                result.validFrom() != null
                        ? result.validFrom().toString()
                        : null,

                // New extended expiry
                metadata.getValidUntil() != null
                        ? metadata.getValidUntil().toString()
                        : null,

                // This newly created document is not overridden
                null
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

        NdaLatestResult result =
                ndaLifecycleService.getLatestNda(visitorId);

        DocumentMetadata metadata = result.metadata();

        DocumentResponse response = new DocumentResponse(
                metadata.getId(),
                metadata.getDocumentPath(),

                // Actual creation/upload time
                metadata.getCreatedAt() != null
                        ? metadata.getCreatedAt().toString()
                        : null,

                // Valid From = original NDA's createdAt
                result.validFrom() != null
                        ? result.validFrom().toString()
                        : null,

                // Valid Until
                metadata.getValidUntil() != null
                        ? metadata.getValidUntil().toString()
                        : null,

                // Latest active NDA should normally be null
                metadata.getOverwrittenBy()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{visitorId}/history")
    public ResponseEntity<List<DocumentResponse>> getNdaHistory(
            @PathVariable String visitorId
    ) {
        return ResponseEntity.ok(
                ndaLifecycleService.getNdaHistory(visitorId)
        );
    }
}
