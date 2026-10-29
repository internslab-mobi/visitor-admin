package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.QrBadgeRequest;
import com.adminvisitor.dto.responsedto.QrBadgeResponse;
import com.adminvisitor.dto.responsedto.QrScanResponse;

import java.time.LocalDateTime;

public interface QrMicroserviceClient {

    QrBadgeResponse generateQrBadge(QrBadgeRequest request);
    void invalidateBadgeOnCheckout(
            String visitId,
            LocalDateTime checkoutTime
    );
    QrScanResponse processScan(String token);

}
