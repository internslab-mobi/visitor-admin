
package com.adminvisitor.util;

import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
@NoArgsConstructor
public final class DateTimeFormats {

    public static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");


}
