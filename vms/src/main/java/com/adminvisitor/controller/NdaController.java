package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.service.NdaService;
import com.adminvisitor.mapper.DocumentMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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

        DocumentMetadata metadata = ndaService.uploadNewNda(
                visitorId,
                ndaFile,
                validUntil
        );

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
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

        DocumentMetadata metadata = ndaService.extendNdaValidity(
                visitorId,
                supportingDocument,
                newValidUntil
        );

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
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

        DocumentMetadata metadata = ndaService.getLatestNda(visitorId);

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
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

        return ResponseEntity.ok(
                ndaService.getNdaHistory(visitorId)
        );
    }
}
