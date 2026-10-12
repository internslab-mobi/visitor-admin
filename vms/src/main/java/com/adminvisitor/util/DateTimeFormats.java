
package com.adminvisitor.util;

import java.time.format.DateTimeFormatter;

public final class DateTimeFormats {

    public static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private DateTimeFormats() {
        // Prevent instantiation
    }
}
