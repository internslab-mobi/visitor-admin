package com.adminvisitor.dto.requestdto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record NdaUploadRequest(
        MultipartFile ndaFile,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate validUntil
) {
}
