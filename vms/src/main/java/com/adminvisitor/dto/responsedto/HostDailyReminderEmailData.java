package com.adminvisitor.dto.responsedto;

import java.util.List;

public record HostDailyReminderEmailData(
        String hostEmail,
        String hostName,
        String reminderDate,
        List<VisitReminderItem> visits
) {

    public record VisitReminderItem(
            String visitorName,
            String visitReference,
            String purpose,
            String expectedArrival,
            String expectedDeparture,
            String visitorType
    ) {}
}