

package com.qrservice.service;

import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.dto.QrScanResponse;

public interface QrService {

    QrBadgeResponse generateQrBadge(QrBadgeRequest request);

    QrScanResponse processScan(String token);

    void invalidateBadgeOnCheckout(
            CheckoutBadgeInvalidationRequest request
    );
}
