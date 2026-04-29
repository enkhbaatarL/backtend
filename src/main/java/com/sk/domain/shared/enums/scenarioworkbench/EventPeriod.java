package com.sk.domain.shared.enums.scenarioworkbench;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventPeriod {

    RECENT_7_DAYS("RECENT_7_DAYS", "최근 7일"),
    ONE_MONTH("ONE_MONTH", "1개월"),
    THREE_MONTHS("THREE_MONTHS", "3개월"),
    SIX_MONTHS("SIX_MONTHS", "6개월"),
    TWELVE_MONTHS("TWELVE_MONTHS", "12개월"),
    ALL("ALL", "전체 기간"),
    CUSTOM("CUSTOM", "사용자 지정");

    private final String code;
    private final String label;

    public Optional<LocalDateTime> calcStartDateTime(LocalDateTime now, LocalDate customStart) {
        return switch (this) {
            case ALL -> Optional.empty();
            case CUSTOM -> Optional.ofNullable(customStart).map(LocalDate::atStartOfDay);
            case RECENT_7_DAYS -> Optional.of(now.minusDays(7));
            case ONE_MONTH -> Optional.of(now.minusMonths(1));
            case THREE_MONTHS -> Optional.of(now.minusMonths(3));
            case SIX_MONTHS -> Optional.of(now.minusMonths(6));
            case TWELVE_MONTHS -> Optional.of(now.minusMonths(12));
        };
    }

    public Optional<LocalDateTime> calcEndDateTime(LocalDate customEnd) {
        if (this != CUSTOM || customEnd == null) {
            return Optional.empty();
        }
        return Optional.of(customEnd.plusDays(1).atStartOfDay());
    }

}
