package com.sk.domain.shared.enums.topic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.function.Function;
import lombok.Getter;

@Getter
public enum DateRangeOption {
    LAST_7_DAYS("LAST_7_DAYS", "최근 7일", now -> now.minusDays(7)),
    LAST_30_DAYS("LAST_30_DAYS", "최근 30일", now -> now.minusDays(30)),
    LAST_60_DAYS("LAST_60_DAYS", "최근 60일", now -> now.minusDays(60)),
    LAST_12_MONTHS("LAST_12_MONTHS", "최근 12개월", now -> now.minusMonths(12)),
    ALL_TIME("ALL_TIME", "전체 기간", now -> LocalDate.of(1970, 1, 1)); // 시스템 시작일 등 기준점 설정

    private final String code;
    private final String description;
    private final Function<LocalDate, LocalDate> startDateCalculator;

    DateRangeOption(String code, String description, Function<LocalDate, LocalDate> startDateCalculator) {
        this.code = code;
        this.description = description;
        this.startDateCalculator = startDateCalculator;
    }

    // API 파라미터(String)를 Enum으로 변환 (Jackson)
    @JsonCreator
    public static DateRangeOption fromCode(String code) {
        for (DateRangeOption option : DateRangeOption.values()) {
            if (option.code.equalsIgnoreCase(code)) {
                return option;
            }
        }
        return LAST_30_DAYS; // 기본값 설정
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    // 기준일부터 시작 날짜를 계산하는 메서드
    public LocalDate getStartDate(LocalDate referenceDate) {
        return this.startDateCalculator.apply(referenceDate);
    }
}
