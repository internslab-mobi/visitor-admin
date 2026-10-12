package com.adminvisitor.auth.dto.responsedto;

public record VerifyOtpResponse(
        String resetToken,
        String message
) {
}
