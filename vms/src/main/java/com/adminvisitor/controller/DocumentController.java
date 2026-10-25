package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.service.DocumentService;
import com.adminvisitor.service.NdaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;


/**
 * REST controller for managing visitor documents.
 *
 * <p>Provides APIs for viewing and downloading NDA documents,
 * retrieving visitor document records, uploading proof documents,
 * retrieving visitor photos, viewing individual documents, and
 * deleting document metadata records.</p>
 *
 * <p>Delegates document operations to {@link DocumentService}
 * and NDA-related file retrieval to {@link NdaService}.</p>
 *
 * @author Anisha Z
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(
        name = "Document Management",
        description = "APIs for uploading, viewing, downloading, and deleting visitor documents"
)
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;
    private final NdaService ndaService;

    /**
     * Downloads or displays the NDA document for a visitor.
     * @param visitorId unique identifier of the visitor
     * @return NDA document as a resource
     */
    @GetMapping("/{visitorId}/nda/download")
    @Operation(
            summary = "Download visitor NDA",
            description = "Retrieves the latest NDA document for the specified visitor for inline viewing."
    )
    public ResponseEntity<Resource> downloadNda(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId

    ) {

        Resource resource = ndaService.getNdaFile(visitorId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\""
                )
                .body(resource);
    }

    /**
     * Retrieves all documents associated with a visitor.
     *
     * @param visitorId unique identifier of the visitor
     * @return list of document details
     */
    @GetMapping("/visitor/{visitorId}")
    @Operation(
            summary = "Get all visitor documents",
            description = "Retrieves all document metadata records associated with the specified visitor."
    )
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId

    ) {

        return ResponseEntity.ok(
                documentService.getAllDocuments(visitorId)
        );
    }

    /**
     * Uploads proof documents for a visitor.
     *
     * @param visitorId unique identifier of the visitor
     * @param files list of proof document files to upload
     * @return list of uploaded document details
     */
    @PostMapping(
            value = "/proof-documents/upload/{visitorId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Upload proof documents",
            description = "Uploads one or more proof documents for the specified visitor and returns their document details."
    )
    public ResponseEntity<List<DocumentResponse>> uploadProofDocuments(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId,

            @Parameter(
                    name = "files",
                    description = "One or more proof document files to upload",
                    required = true
            )
            @RequestParam("files") List<MultipartFile> files

    ) {

        List<DocumentMetadata> uploadedDocuments =
                documentService.uploadProofDocuments(
                        visitorId,
                        files
                );

        List<DocumentResponse> response = uploadedDocuments.stream()
                .map(documentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the latest photo of a visitor.
     *
     * @param visitorId unique identifier of the visitor
     * @return visitor photo as a resource
     */
    @GetMapping("/visitor/{visitorId}/photo")
    @Operation(
            summary = "Get visitor photo",
            description = "Retrieves the latest photo associated with the specified visitor for inline viewing."
    )
    public ResponseEntity<Resource> getVisitorPhoto(

            @Parameter(
                    name = "visitorId",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId

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

    /**
     * Retrieves a document by its unique document identifier.
     *
     * @param documentId unique identifier of the document
     * @return document as a resource for inline viewing
     */
    @GetMapping("/{documentId}/view")
    @Operation(
            summary = "View document",
            description = "Retrieves a document using its unique identifier and returns the file for inline viewing."
    )
    public ResponseEntity<Resource> viewDocument(

            @Parameter(
                    name = "documentId",
                   required = true,
                    example = "DOC001"
            )
            @PathVariable("documentId") String documentId

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

    /**
     * Deletes a document metadata record.
     *
     * @param metadataId unique identifier of the document metadata
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{metadataId}")
    @Operation(
            summary = "Delete document",
            description = "Deletes a document using its unique metadata identifier and returns an empty response."
    )
    public ResponseEntity<Void> deleteDocument(

            @Parameter(
                    name = "metadataId",
                    required = true,
                    example = "DOC001"
            )
            @PathVariable("metadataId") String metadataId

    ) {

        documentService.deleteDocument(metadataId);

        return ResponseEntity.noContent().build();
    }
}
