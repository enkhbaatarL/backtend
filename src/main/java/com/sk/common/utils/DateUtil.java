package com.sk.common.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.commons.lang3.tuple.Pair;

import com.sk.domain.shared.enums.chart.Range;

/**
 * LocalDate 관련 유틸리티 클래스
 */
public final class DateUtil {

    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");
    public static final DateTimeFormatter YYYY_MMM_ENG = DateTimeFormatter.ofPattern("yyyy-MMM", Locale.ENGLISH);

    private DateUtil() {
    }

    /**
     * LocalDate를 "yyyyMMdd" 문자열로 변환
     */
    public static String toYmd(LocalDate date) {
        return date == null ? null : date.format(YYYYMMDD);
    }

    /**
     * LocalDateTime을 "yyyyMMdd" 문자열로 변환
     */
    public static String toYmd(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(YYYYMMDD);
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
     * 현재 날짜를 "yyyyMMdd"로 반환
     */
    public static String currentYmd() {
        return LocalDate.now().format(YYYYMMDD);
    }

    /**
     * 10년 후 날짜를 "yyyyMMdd"로 반환
     */
    public static String after10yYmd() {
        return LocalDate.now().plusYears(10).format(YYYYMMDD);
    }

    /**
     * 현재 날짜를 "yyyy-MM-dd"로 반환
     */
    public static String currentYmdHyphen() {
        return LocalDate.now().format(YYYY_MM_DD);
    }

    /**
     * 현재 날짜를 "yyyy-MM-dd HH:mm:ss"로 반환
     */
    public static String currentYmdHms() {
        return LocalDate.now().format(YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * string 날짜를 LocalDate 타입으로 반환
     */
    public static LocalDate parseDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
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

    public static String toYmd(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank())
            return null;

        try {
            // ISO-8601 형식 처리 (예: 2024-01-01T00:00)
            LocalDateTime ldt = LocalDateTime.parse(dateTimeStr);
            return ldt.format(YYYYMMDD);

        } catch (DateTimeParseException e1) {
            try {
                // 기존 포맷 시도 (예: 2024-01-01 00:00:00)
                String trimmed = dateTimeStr.length() > 19
                        ? dateTimeStr.substring(0, 19)
                        : dateTimeStr;
                LocalDateTime ldt = LocalDateTime.parse(trimmed, YYYY_MM_DD_HH_MM_SS);
                return ldt.format(YYYYMMDD);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format: " + dateTimeStr);
            }
        }
    }

    public static Pair<String, String> getDateRange(Range range) {

        LocalDate endDate = LocalDate.now();

        LocalDate startDate = switch (range) {
            case ONE_WEEK -> endDate.minusWeeks(1);
            case ONE_MONTH -> endDate.minusMonths(1);
            case THREE_MONTHS -> endDate.minusMonths(3);
            case SIX_MONTHS -> endDate.minusMonths(6);
            case ONE_YEAR -> endDate.minusYears(1);
            case TWO_YEARS -> endDate.minusYears(2);
            default -> endDate;
        };

        String from = toYmd(startDate);
        String to = toYmd(endDate);
        return Pair.of(from, to);
    }

    /**
     * 마지막 날짜를 "yyyyMMdd"로 반환
     */
    public static String endYmd() {
        return "99991231";
    }

    /**
     * ISO 형식의 날짜 문자열을 LocalDate로 변환
     */
    public static LocalDate fromIsoToLocalDate(String dateStr) {
        return dateStr == null ? null : LocalDateTime.parse(dateStr, ISO_LOCAL_DATE_TIME_MINUTES).toLocalDate();
    }

    /**
     * LocalDate를 "yyyyMMdd" 문자열로 변환
     */
    public static String toIso(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MIDNIGHT).format(ISO_LOCAL_DATE_TIME_MINUTES);
    }

    /**
     * LocalDateTime을 "yyyyMM" 문자열로 변환
     */
    public static String toYearMonth(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(YYYYMM);
    }

    /**
     * LocalDate를 "yyyyMM" 문자열로 변환
     */
    public static String toYearMonth(LocalDate date) {
        return date == null ? null : date.format(YYYYMM);
    }

    /**
     * "yyyy-MMM" 문자열을 "yyyyMM" 문자열로 변환
     */
    public static String fromYyyyMmmToYyyyMm(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            YearMonth ym = YearMonth.parse(dateStr.trim(), YYYY_MMM_ENG);
            return ym.format(YYYYMM);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format, expected yyyy-MMM: " + dateStr);
        }
    }

}
