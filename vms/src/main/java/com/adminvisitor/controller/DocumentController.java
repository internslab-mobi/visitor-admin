package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.IdentityProofResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;



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

//    @GetMapping("/{visitorId}/identity-proof")
//    public ResponseEntity<IdentityProofResponse> getIdentityProofs(
//            @PathVariable String visitorId
//    ) {
//        return ResponseEntity.ok(
//                documentService.getIdentityProofs(visitorId)
//        );
//    }




    public ResponseEntity<List<DocumentResponse>> uploadProofDocuments(
            @PathVariable String visitorId,
            @RequestParam("files") List<MultipartFile> files
    ) {

        List<DocumentMetadata> uploadedDocuments =
                documentService.uploadProofDocuments(
                        visitorId,
                        files
                );

        List<DocumentResponse> response =
                uploadedDocuments.stream()
                        .map(metadata ->
                                new DocumentResponse(
                                        metadata.getId(),
                                        metadata.getDocumentPath(),

                                        // Created At
                                        metadata.getCreatedAt() != null
                                                ? metadata.getCreatedAt().toString()
                                                : null,

                                        // Valid From - not applicable for proof documents
                                        null,

                                        // Valid Until - not applicable for proof documents
                                        null,

                                        // Overwritten By - not applicable for proof documents
                                        null
                                )
                        )
                        .toList();

        return ResponseEntity.ok(response);
    }



    @GetMapping(
            "/visitor/{visitorId}/photo"
    )
    public ResponseEntity<Resource> getVisitorPhoto(
            @PathVariable String visitorId
    ) {

        DocumentMetadata metadata =
                documentService.getLatestVisitorPhoto(
                        visitorId
                );

        Resource resource =
                new FileSystemResource(
                        metadata.getDocumentPath()
                );

        if (!resource.exists()) {
            throw new ResourceNotFoundException(
                    "Visitor photo file not found"
            );
        }

        MediaType mediaType =
                MediaTypeFactory
                        .getMediaType(
                                resource.getFilename()
                        )
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                resource.getFilename() +
                                "\""
                )
                .body(resource);
    }



    @GetMapping("/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable String documentId
    ) {

        DocumentMetadata metadata =
                documentService.getDocumentMetadata(documentId);

        Resource resource =
                new FileSystemResource(
                        metadata.getDocumentPath()
                );

        if (!resource.exists()) {
            throw new ResourceNotFoundException(
                    "Document file not found: " + documentId
            );
        }

        MediaType mediaType =
                MediaTypeFactory
                        .getMediaType(resource.getFilename())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                resource.getFilename() +
                                "\""
                )
                .body(resource);
    }


    @DeleteMapping("/{metadataId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String metadataId
    ) {documentService.deleteDocument(metadataId);
        return ResponseEntity.noContent().build();
    }



}