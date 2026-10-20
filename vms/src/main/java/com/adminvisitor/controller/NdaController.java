
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
import com.adminvisitor.mapper.DocumentMapper;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/nda")
@RequiredArgsConstructor
public class NdaController {

    private final NdaService ndaService;
    private final DocumentMapper documentMapper;
    @PostMapping(
            value = "/{visitorId}/upload-new",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> uploadNewNda(
            @PathVariable String visitorId,
            @RequestParam("ndaFile") MultipartFile ndaFile,
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

        DocumentMetadata metadata = ndaService.extendNdaValidity(
                visitorId,
                supportingDocument,
                newValidUntil
        );

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
    }

    @GetMapping("/{visitorId}/latest")
    public ResponseEntity<DocumentResponse> getLatestNda(
            @PathVariable String visitorId
    ) {

        DocumentMetadata metadata = ndaService.getLatestNda(visitorId);

        return ResponseEntity.ok(documentMapper.toResponse(metadata));
    }

    @GetMapping("/{visitorId}/history")
    public ResponseEntity<List<DocumentResponse>> getNdaHistory(
            @PathVariable String visitorId
    ) {
        return ResponseEntity.ok(
                ndaService.getNdaHistory(visitorId)
        );
    }
}
