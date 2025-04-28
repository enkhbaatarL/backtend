package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.enums.chart.Range;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

}
