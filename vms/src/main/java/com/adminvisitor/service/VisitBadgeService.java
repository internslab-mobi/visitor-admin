package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.QrScanResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.BadgeStatus;
import java.time.LocalDateTime;

public interface VisitBadgeService {

    VisitBadge createBadge(Visit visit, String qrToken);

    void invalidateBadgeOnCheckout(
            String visitId,
            LocalDateTime checkoutTime);

    VisitBadge validateQrTokenForUpdate(String qrToken);

    QrScanResponse processScan(String token);

}