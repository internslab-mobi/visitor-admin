package com.adminvisitor.dto.requestdto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record NdaExtensionRequest(
        MultipartFile supportingDocument,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate newValidUntil
) {
}
