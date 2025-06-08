package com.sk.common.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
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
    public static final DateTimeFormatter YYYYMMDDHHMMSS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter YYYY_MM = DateTimeFormatter.ofPattern("yyyy-MM");

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
        return LocalDateTime.now().format(YYYY_MM_DD_HH_MM_SS);
    }

    public static String currentYYYYMMDDHHMMSS() {
        return LocalDateTime.now().format(YYYYMMDDHHMMSS);
    }

    /**
     * 여러 날짜 타입을 모두 LocalDate로 변환하는 메소드
     */
    public static LocalDate parseDate(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate ld) return ld;
        if (dateObj instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (dateObj instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (dateObj instanceof Integer i) {
            // int 연도 (ex: 2024)
            if (i >= 1000 && i <= 9999) return LocalDate.of(i, 1, 1);
            return null;
        }
        String s = dateObj.toString().trim();

        // 연도: 4자리 숫자 (varchar4, int 문자열 변환 포함)
        if (s.matches("\\d{4}")) {
            int year = Integer.parseInt(s);
            return LocalDate.of(year, 1, 1);
        }
        // 월: 6자리 숫자 (varchar6)
        if (s.matches("\\d{6}")) {
            int year = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(4, 6));
            return LocalDate.of(year, month, 1);
        }
        // 일: 8자리 숫자 (varchar8)
        if (s.matches("\\d{8}")) {
            return LocalDate.parse(s, DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        }
        // yyyy-MM-dd
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(s);
        }
        // yyyy-MM
        if (s.matches("\\d{4}-\\d{2}")) {
            int year = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(5, 7));
            return LocalDate.of(year, month, 1);
        }
        // yyyy-MM-dd[ T]HH:mm:ss[.SSS][XXX] 등: 앞 10글자만
        if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return LocalDate.parse(s.substring(0, 10));
        }

        // datetime 타입 문자열도 커버: yyyy/MM/dd, yyyy.MM.dd 등
        if (s.matches("\\d{4}[./]\\d{2}[./]\\d{2}")) {
            String replaced = s.replace('.', '-').replace('/', '-');
            return LocalDate.parse(replaced);
        }

        return null;
    }

    public static String format(LocalDate date, PeriodUnitSeparateCd unit) {
        return switch (unit) {
            case M -> toYearMonth(date);
            default -> toYmd(date);
        };
    }

    public static Pair<String, String> getDateRange(Range range) {

        LocalDate endDate = LocalDate.now();

        LocalDate startDate = switch (range) {
            case ONE_WEEK -> endDate.minusWeeks(1).plusDays(1);
            case ONE_MONTH -> endDate.minusMonths(1).plusDays(1);
            case THREE_MONTHS -> endDate.minusMonths(3).plusDays(1);
            case SIX_MONTHS -> endDate.minusMonths(6).plusDays(1);
            case ONE_YEAR -> endDate.minusYears(1).plusDays(1);
            case TWO_YEARS -> endDate.minusYears(2).plusDays(1);
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
        return dateStr == null ? null :
            (dateStr.length() == 6) ? YearMonth.parse(dateStr, YYYYMM).atDay(1) :
            (dateStr.length() == 8) ? LocalDate.parse(dateStr, YYYYMMDD) :
                LocalDateTime.parse(dateStr, ISO_LOCAL_DATE_TIME_MINUTES).toLocalDate();
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
     * "yyyyMM" 문자열을 LocalDate로 변환 (202501 -> LocalDate 2025-01-01)
     */
    public static LocalDate fromYearMonth(String yearMonth) {
        return yearMonth == null ? null : YearMonth.parse(yearMonth, YYYYMM).atDay(1);
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

    public static String minusDays(String dateStr, int days) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        LocalDate date = parseDate(dateStr);
        if (date == null) {
            return null;
        }
        return toYmd(date.minusDays(days));
    }


    /**
     * LocalDateTime을 "yyyy-MM" 문자열로 변환
     */
    public static String toYyyyMm(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(YYYY_MM);
    }

    /**
     * LocalDate를 "yyyy-MM" 문자열로 변환
     */
    public static String toYyyyMm(LocalDate date) {
        return date == null ? null : date.format(YYYY_MM);
    }

    // YYYYMMDD
}
