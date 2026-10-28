package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.service.NdaService;
import com.adminvisitor.mapper.DocumentMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


/**
 * REST controller for managing visitor NDA documents.
 *
 * <p>Provides APIs for uploading new NDA documents, extending NDA
 * validity, retrieving the latest NDA document, and accessing the
 * document history for a specified visitor.</p>
 *
 * <p>Delegates NDA-related business operations to {@link NdaService}
 * and converts document metadata into response DTOs using
 * {@link DocumentMapper}.</p>
 *
 * @author Anisha Z
 */

@Slf4j
@RestController
@RequestMapping("/api/nda")
@RequiredArgsConstructor
@Tag(
        name = "NDA Management",
        description = "APIs for uploading, extending, and retrieving visitor NDA documents"
)
public class NdaController {

    private final NdaService ndaService;
    private final DocumentMapper documentMapper;

    /**
     * Uploads a new NDA document for the specified visitor.
     * @param visitorId unique identifier of the visitor
     * @param ndaFile NDA document to upload
     * @param validUntil validity end date of the NDA
     * @return uploaded NDA document details
     */
    @PostMapping(
            value = "/{visitorId}/upload-new",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Upload a new NDA",
            description = "Uploads a new NDA document for the specified visitor and sets its validity end date. The uploaded document is stored as a new document record."
    )
    public ResponseEntity<DocumentResponse> uploadNewNda(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId,

            @Parameter(
                    name = "ndaFile",
                    description = "NDA document file to upload",
                    required = true
            )
            @RequestParam("ndaFile") MultipartFile ndaFile,

            @Parameter(
                    name = "validUntil",
                    description = "Date until which the NDA remains valid, in YYYY-MM-DD format",
                    required = true,
                    example = "2027-10-22"
            )
            @RequestParam("validUntil")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate validUntil

    ) {
        try {
        DocumentMetadata metadata = ndaService.uploadNewNda(
                visitorId,
                ndaFile,
                validUntil
        );

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
    }catch (ResourceNotFoundException e) {
            log.error("Visitor not found for NDA upload: visitorId={}", visitorId);
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Invalid NDA upload request for visitorId={}: {}",
                    visitorId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Error uploading NDA for visitorId={}", visitorId, e);
            throw e;
        }
    }

    /**
     * Extends the NDA validity for the specified visitor.
     * @param visitorId unique identifier of the visitor
     * @param supportingDocument supporting document for the extension
     * @param newValidUntil new validity end date
     * @return document details associated with the extension
     */
    @PostMapping(
            value = "/{visitorId}/extend",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Extend NDA validity",
            description = "Uploads a supporting document and requests an extension of the specified visitor's NDA validity until the supplied new validity date."
    )
    public ResponseEntity<DocumentResponse> extendNdaValidity(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId,

            @Parameter(
                    name = "supportingDocument",
                    description = "Supporting document required for the NDA validity extension",
                    required = true
            )
            @RequestParam("supportingDocument") MultipartFile supportingDocument,

            @Parameter(
                    name = "newValidUntil",
                    description = "New NDA validity end date, in YYYY-MM-DD format",
                    required = true,
                    example = "2028-10-22"
            )
            @RequestParam("newValidUntil")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate newValidUntil

    ) {
        try {
        DocumentMetadata metadata = ndaService.extendNdaValidity(
                visitorId,
                supportingDocument,
                newValidUntil
        );

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
    }catch (ResourceNotFoundException e) {
            log.error("Visitor or NDA not found for extension: visitorId={}",
                    visitorId);
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Invalid NDA extension request for visitorId={}: {}",
                    visitorId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Error extending NDA validity for visitorId={}",
                    visitorId, e);
            throw e;
        }
    }

    /**
     * Retrieves the latest NDA document for a visitor.
     *
     * @param visitorId unique identifier of the visitor
     * @return latest NDA document details
     */
    @GetMapping(
            value = "/{visitorId}/latest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get the latest NDA",
            description = "Retrieves the latest NDA document details for the specified visitor."
    )
    public ResponseEntity<DocumentResponse> getLatestNda(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId

    ) {
        try {
        DocumentMetadata metadata = ndaService.getLatestNda(visitorId);

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
    }catch (ResourceNotFoundException e) {
            log.error("Latest NDA not found for visitorId={}", visitorId);
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Invalid latest NDA request for visitorId={}: {}",
                    visitorId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Error retrieving latest NDA for visitorId={}",
                    visitorId, e);
            throw e;
        }
    }

    /**
     * Retrieves the NDA document history for a visitor.
     *
     * @param visitorId unique identifier of the visitor
     * @return list of NDA document records, including previous documents and extensions
     */
    @GetMapping(
            value = "/{visitorId}/history",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get NDA history",
            description = "Retrieves the available NDA document history for the specified visitor, including previous NDA records and validity extensions represented by separate document records."
    )
    public ResponseEntity<List<DocumentResponse>> getNdaHistory(

            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId

    ) {
        try {
            return ResponseEntity.ok(
                    ndaService.getNdaHistory(visitorId)
            );

        } catch (ResourceNotFoundException e) {
            log.error("NDA history not found for visitorId={}", visitorId);
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Invalid NDA history request for visitorId={}: {}",
                    visitorId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Error retrieving NDA history for visitorId={}",
                    visitorId, e);
            throw e;
        }
    }
}
