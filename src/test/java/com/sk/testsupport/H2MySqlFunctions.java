package com.sk.testsupport;

import java.sql.Date;
import java.time.format.DateTimeFormatter;

public final class H2MySqlFunctions {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private H2MySqlFunctions() {
    }

    public static String dateFormat(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        if (!"%Y%m%d".equals(pattern)) {
            throw new IllegalArgumentException("Unsupported DATE_FORMAT pattern: " + pattern);
        }
        return date.toLocalDate().format(YYYYMMDD);
    }
}
