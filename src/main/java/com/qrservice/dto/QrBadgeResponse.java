
package com.qrservice.dto;

public record QrBadgeResponse(
        String token,
        String qrCode
) {
}
