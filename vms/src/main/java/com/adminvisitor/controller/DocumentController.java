package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.IdentityProofResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


     // Upload a signed NDA for a visitor.

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

    //  Retrieve the latest NDA for a visitor.

    @GetMapping("/{visitorId}/nda")
    public ResponseEntity<DocumentResponse> getLatestNda(
            @PathVariable String visitorId
    ) {

        Document document =
                documentService.getLatestNda(visitorId);

        DocumentResponse response =
                new DocumentResponse(
                        document.getId(),
                      //  document.getVisitor().getId(),
                        document.getNdaDocument(),
                        document.getCreatedAt().toString()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{visitorId}/nda/download")
    public ResponseEntity<Resource> downloadNda(
            @PathVariable String visitorId) {

        Resource resource = documentService.getNdaFile(visitorId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\""
                )
                .body(resource);
    }

    @GetMapping("/visitor/{visitorId}")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @PathVariable String visitorId) {

        return ResponseEntity.ok(
                documentService.getAllDocuments(visitorId)
        );
    }

    @GetMapping("/{visitorId}/identity-proof")
    public ResponseEntity<IdentityProofResponse> getIdentityProofs(
            @PathVariable String visitorId
    ) {
        return ResponseEntity.ok(
                documentService.getIdentityProofs(visitorId)
        );
    }
}