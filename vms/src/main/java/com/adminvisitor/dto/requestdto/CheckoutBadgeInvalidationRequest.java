
package com.adminvisitor.dto.requestdto;

import java.time.LocalDateTime;

public record CheckoutBadgeInvalidationRequest(
        String visitId,
        LocalDateTime checkoutTime
) {
}
