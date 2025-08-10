package com.sk.domain.shared.enums.chart;

import com.sk.common.utils.DateUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * 지표 계산(Window) 룩백 정의.<br/>
 * <ul>
 *     <li>windowSize : 지표 계산에 필요한 "버킷" 개수</li>
 *     <li>neededPastDataPoints : 현재 버킷을 제외한 과거 포인트 수 (= windowSize-1)</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum LookBack {

    W_SIZE_1(1),
    W_SIZE_5(5),
    W_SIZE_20(20),
    W_SIZE_60(60),
    W_SIZE_120(120);

    private final int windowSize;

    /**
     * 현재 버킷을 제외하고 추가로 필요한 과거 데이터 포인트 수
     */
    public int getNeededPastDataPoints() {
        return windowSize > 0 ? windowSize - 1 : 0;
    }

    public String getLookBackStartDateStr(LocalDate startDate, Interval interval, int spareBuckets) {
        return DateUtil.toYmd(getLookBackStartDate(startDate, interval, spareBuckets));
    }

    /**
     * rangeStartInclusive 에서 가장 먼 룩백 시작일을 계산.
     *
     * @param startDate    range 의 시작일(포함)
     * @param interval     집계 단위
     * @param spareBuckets 휴일/결측 대비로 <b>추가로 확보</b>할 여유 "버킷" 개수
     * @return earliestDate   inclusive (이 날짜부터 raw 를 조회하면 windowSize+spareBuckets 버킷을 만들 수 있다)
     */
    public LocalDate getLookBackStartDate(LocalDate startDate, Interval interval, int spareBuckets) {
        int totalBuckets = getNeededPastDataPoints() + Math.max(spareBuckets, 0);
        return retreat(startDate, interval, totalBuckets);
    }

    /**
     * 최대 windowSize enum 반환
     */
    public static LookBack max(LookBack a, LookBack b) {
        return a.windowSize >= b.windowSize ? a : b;
    }

    /**
     * date 에서 interval 을 n 번 뒤로 이동(retreat)한 날짜 반환.
     * ONE_DAY 는 minusDays(n), ONE_WEEK 는 minusWeeks(n) 처럼 매핑.
     */
    private static LocalDate retreat(LocalDate date, Interval interval, int n) {
        return switch (interval) {
            case ONE_DAY -> date.minusDays(n);
            case ONE_WEEK -> date.minusWeeks(n);
            case TWO_WEEK -> date.minusWeeks(n * 2L);
            case ONE_MONTH -> date.minusMonths(n);
            case THREE_MONTH -> date.minusMonths(n * 3L);
            case SIX_MONTH -> date.minusMonths(n * 6L);
            case ONE_YEAR -> date.minusYears(n);
        };
    }

}
