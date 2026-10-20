package com.adminvisitor.dto.responsedto;



import java.time.LocalDateTime;

public record QrScanResponse(
        String message,
        String action,
        LocalDateTime scannedAt
) {
}
