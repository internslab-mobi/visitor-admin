
package com.qrservice.dto;

import java.time.LocalDateTime;

public record CheckoutBadgeInvalidationRequest(
        String visitId,
        LocalDateTime checkoutTime
) {
}
