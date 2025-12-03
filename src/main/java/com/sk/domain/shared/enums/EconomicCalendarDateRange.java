package com.sk.domain.shared.enums;

import com.sk.common.utils.DateUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 경제 지표 달력 날짜 범위 Enum
 */
@Getter
@RequiredArgsConstructor
public enum EconomicCalendarDateRange {

    TODAY("TODAY", "오늘"),
    YESTERDAY("YESTERDAY", "어제"),
    TOMORROW("TOMORROW", "내일"),
    THISWEEK("THISWEEK", "이번 주"),
    NEXTWEEK("NEXTWEEK", "다음 주");

    private final String code;
    private final String description;

    /**
     * 코드로 Enum을 찾습니다.
     *
     * @param code 코드
     * @return EconomicCalendarDateRange 또는 null
     */
    public static EconomicCalendarDateRange findByCode(String code) {
        if (code == null) {
            return null;
        }
        for (EconomicCalendarDateRange range : values()) {
            if (range.code.equalsIgnoreCase(code)) {
                return range;
            }
        }
        return null;
    }

    /**
     * 날짜 범위를 계산하여 반환합니다.
     * 단일 날짜인 경우 시작일과 종료일이 동일합니다.
     *
     * @return Pair<시작일(yyyy-MM-dd), 종료일(yyyy-MM-dd)>
     */
    public Pair<String, String> getDateRange() {
        LocalDate today = LocalDate.now();
        today = LocalDate.parse("20251028", DateTimeFormatter.ofPattern("yyyyMMdd"));

        return switch (this) {
            case TODAY -> {
                String date = DateUtil.toYmdHyphen(today);
                yield Pair.of(date, date);
            }
            case YESTERDAY -> {
                LocalDate yesterday = today.minusDays(1);
                String date = DateUtil.toYmdHyphen(yesterday);
                yield Pair.of(date, date);
            }
            case TOMORROW -> {
                LocalDate tomorrow = today.plusDays(1);
                String date = DateUtil.toYmdHyphen(tomorrow);
                yield Pair.of(date, date);
            }
            case THISWEEK -> {
                // 이번 주 월요일부터 일요일까지
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                LocalDate sunday = today.with(DayOfWeek.SUNDAY);
                yield Pair.of(DateUtil.toYmdHyphen(monday), DateUtil.toYmdHyphen(sunday));
            }
            case NEXTWEEK -> {
                // 다음 주 월요일부터 일요일까지
                LocalDate nextMonday = today.with(DayOfWeek.MONDAY).plusWeeks(1);
                LocalDate nextSunday = nextMonday.with(DayOfWeek.SUNDAY);
                yield Pair.of(DateUtil.toYmdHyphen(nextMonday), DateUtil.toYmdHyphen(nextSunday));
            }
        };
    }
}


