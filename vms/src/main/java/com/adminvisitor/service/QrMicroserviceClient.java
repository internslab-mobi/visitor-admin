package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.CheckoutBadgeInvalidationRequest;
import com.adminvisitor.dto.requestdto.QrBadgeRequest;
import com.adminvisitor.dto.responsedto.QrBadgeResponse;

import java.time.LocalDateTime;

public interface QrMicroserviceClient {

    QrBadgeResponse generateQrBadge(QrBadgeRequest request);
    void invalidateBadgeOnCheckout(
            String visitId,
            LocalDateTime checkoutTime
    );
}
