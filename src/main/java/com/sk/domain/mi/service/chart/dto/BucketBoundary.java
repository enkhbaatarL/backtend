package com.sk.domain.mi.service.chart.dto;

import com.sk.domain.shared.enums.chart.Interval;
import lombok.Builder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 버킷 경계를 나타내는 내부 레코드 (시작일 포함, 종료일 미포함)
 *
 * @param InclusiveStart
 * @param exclusiveEnd
 */
@Builder
public record BucketBoundary(

    LocalDate InclusiveStart,

    LocalDate exclusiveEnd,

    Interval interval

) {

    public static BucketBoundary of(LocalDate start, LocalDate exclusiveEnd, Interval interval) {
        return BucketBoundary.builder()
            .InclusiveStart(start)
            .exclusiveEnd(exclusiveEnd)
            .interval(interval)
            .build();
    }

    static List<BucketBoundary> build(LocalDate anchorExclusiveEnd, LocalDate inclusiveStart, Interval interval) {

        List<BucketBoundary> list = new ArrayList<>();

        LocalDate currentAnchor = anchorExclusiveEnd;
        while (!currentAnchor.isBefore(inclusiveStart.plusDays(1))) {
            LocalDate currentInclusiveStart = retreat(currentAnchor, interval);
            if (currentInclusiveStart.isBefore(inclusiveStart)) currentInclusiveStart = inclusiveStart;
            list.add(BucketBoundary.of(currentInclusiveStart, currentAnchor, interval));
            currentAnchor = currentInclusiveStart;
            if (currentInclusiveStart.equals(inclusiveStart)) break;
        }
        return list; // 최신→과거 역순
    }

    /**
     * 룩백 데이터(dates)가 rangeStart 이전에 존재하면, rangeStart‑1 을 새로운 anchor 로 삼아
     * 추가 버킷(extended boundaries) 를 만든다.
     */
    public static List<BucketBoundary> build(List<LocalDate> dates, LocalDate anchorExclusiveEnd, LocalDate inclusiveStart, Interval interval) {

        // 1) display용 boundaries
        List<BucketBoundary> boundaries = build(anchorExclusiveEnd, inclusiveStart, interval);

        // 2) earliest raw date 확인(LOOKBACK을 위한 데이터)
        LocalDate earliest = dates.stream().min(LocalDate::compareTo).orElse(inclusiveStart);

        if (!earliest.isBefore(inclusiveStart)) {
            // 룩백 없음 → 그대로 리턴
            return boundaries;
        }

        // 3) 추가 버킷 생성: 새로운 anchor = rangeStartInclusive.minusDays(1)
        LocalDate extEnd = inclusiveStart;
        while (!extEnd.isBefore(earliest.plusDays(1))) {        // still covers at least 1 day
            LocalDate extStart = retreat(extEnd, interval);
            if (extStart.isBefore(earliest)) extStart = earliest;
            boundaries.add(BucketBoundary.of(extStart, extEnd, interval));
            if (extStart.equals(earliest)) break;
            extEnd = extStart;
        }
        return boundaries;
    }

    private static LocalDate retreat(LocalDate date, Interval i) {
        return switch (i) {
            case ONE_DAY -> date.minusDays(1);
            case ONE_WEEK -> date.minusWeeks(1);
            case TWO_WEEK -> date.minusWeeks(2);
            case ONE_MONTH -> date.minusMonths(1);
            case THREE_MONTH -> date.minusMonths(3);
            case SIX_MONTH -> date.minusMonths(6);
            case ONE_YEAR -> date.minusYears(1);
        };
    }

}
