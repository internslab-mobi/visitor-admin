
package com.qrservice.dto;

import java.time.LocalDateTime;

public record QrScanResponse(
        String message,
        String action,
        LocalDateTime scannedAt
) {}