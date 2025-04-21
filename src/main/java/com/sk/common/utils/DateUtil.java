package com.sk.common.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalDate 관련 유틸리티 클래스
 */
public final class DateUtil {

    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtil() {
    }

    /**
     * LocalDate를 "yyyyMMdd" 문자열로 변환
     */
    public static String toYmd(LocalDate date) {
        return date == null ? null : date.format(YYYYMMDD);
    }

    /**
     * LocalDate를 "yyyy-MM-dd" 문자열로 변환
     */
    public static String toYmdHyphen(LocalDate date) {
        return date == null ? null : date.format(YYYY_MM_DD);
    }

    /**
     * "yyyyMMdd" 문자열을 LocalDate로 변환
     */
    public static LocalDate fromYmd(String dateStr) {
        return dateStr == null ? null : LocalDate.parse(dateStr, YYYYMMDD);
    }

    /**
     * "yyyy-MM-dd" 문자열을 LocalDate로 변환
     */
    public static LocalDate fromYmdHyphen(String dateStr) {
        return dateStr == null ? null : LocalDate.parse(dateStr, YYYY_MM_DD);
    }

    /**
     * LocalDate를 사용자 지정 포맷 문자열로 변환
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 사용자 지정 포맷 문자열을 LocalDate로 변환
     */
    public static LocalDate parse(String dateStr, String pattern) {
        if (dateStr == null || pattern == null) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 현재 날짜를 "yyyyMMdd"로 반환
     */
    public static String currentYmd() {
        return LocalDate.now().format(YYYYMMDD);
    }

    /**
     * 현재 날짜를 "yyyy-MM-dd"로 반환
     */
    public static String currentYmdHyphen() {
        return LocalDate.now().format(YYYY_MM_DD);
    }

    /**
     * 날짜를 LocalDate(yyyy-MM-dd)로 반환
     */
    public static LocalDate parseDate(Object dateObj) {
        if (dateObj instanceof LocalDate ld) {
            return ld;
        } else if (dateObj instanceof LocalDateTime ldt) {
            return ldt.toLocalDate();
        } else if (dateObj instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        } else {
            String s = dateObj.toString();
            String d = s.length() >= 10 ? s.substring(0, 10) : s;
            return DateUtil.fromYmdHyphen(d);
        }
    }


    /**
     * 마지막 날짜를 "yyyyMMdd"로 반환
     */
    public static String endYmd() {
        return "99991231";
    }

}
