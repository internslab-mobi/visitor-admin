package com.adminvisitor.controller;

import com.adminvisitor.entity.Document;
import com.adminvisitor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload a signed NDA for a visitor.
     */
    @PostMapping(
            value = "/{visitorId}/nda",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Document> uploadSignedNda(
            @PathVariable String visitorId,
            @RequestParam("file") MultipartFile file
    ) {

        Document document =
                documentService.uploadSignedNda(
                        visitorId,
                        file
                );

        return ResponseEntity.ok(document);
    }
    /**
     * Retrieve the latest NDA for a visitor.
     */
    @GetMapping("/{visitorId}/nda")
    public ResponseEntity<Document> getLatestNda(
            @PathVariable String visitorId
    ) {

        Document document =
                documentService.getLatestNda(
                        visitorId
                );

        return ResponseEntity.ok(document);
    }
}