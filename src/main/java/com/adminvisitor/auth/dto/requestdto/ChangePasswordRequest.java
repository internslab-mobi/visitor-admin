package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword
) {
        public ChangePasswordRequest(String currentPassword, String newPassword) {
                this(currentPassword, newPassword, newPassword);
        }
}
