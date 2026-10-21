package com.adminvisitor.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class VisitReferenceUtil {

    private static final DateTimeFormatter VISIT_REFERENCE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private VisitReferenceUtil() {
        // Prevent instantiation
    }

    public static String generateVisitReference() {
        String timestamp = LocalDateTime.now()
                .format(VISIT_REFERENCE_FORMATTER);

        String randomPart = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "VIS-" + timestamp + "-" + randomPart;
    }
}