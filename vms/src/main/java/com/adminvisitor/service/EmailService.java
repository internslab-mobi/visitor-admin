package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.BadgeEmailData;
import com.adminvisitor.dto.responsedto.CancellationEmailData;
import com.adminvisitor.dto.responsedto.HostCheckInEmailData;
import com.adminvisitor.entity.Visit;

public interface EmailService {

    void sendVisitConfirmationEmail(Visit visit);

    void sendVisitBadgeEmail(BadgeEmailData data);

    void sendHostCheckInNotification(
            HostCheckInEmailData data);

    void sendVisitCancellationEmail(
            CancellationEmailData data);
}