package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.ExportRequest;
import com.adminvisitor.service.SummaryExportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vms/summary-export")
@RequiredArgsConstructor
public class SummaryExportController {

    private final SummaryExportClient summaryExportClient;

    @PostMapping
    public ResponseEntity<byte[]> exportSummary(
            @RequestBody ExportRequest request) {

        byte[] file = summaryExportClient.export(request);

        boolean isCsv = "csv".equalsIgnoreCase(request.getFormat());

        MediaType contentType = isCsv
                ? MediaType.parseMediaType("text/csv")
                : MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        String extension = isCsv ? "csv" : "xlsx";

        String fileName = request.getFileName();

        if (fileName == null || fileName.isBlank()) {
            fileName = "vms-summary";
        }

        if (!fileName.toLowerCase()
                .endsWith("." + extension)) {
            fileName += "." + extension;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentLength(file.length)
                .body(file);
    }
}
