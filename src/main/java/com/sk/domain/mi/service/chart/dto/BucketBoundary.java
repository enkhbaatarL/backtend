package com.sk.domain.mi.service.chart.dto;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.enums.chart.Interval;
import lombok.Builder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 버킷 경계를 나타내는 내부 레코드 (시작일 포함, 종료일 미포함)
 */
@Builder
public record BucketBoundary(

    LocalDate InclusiveStart,
    LocalDate exclusiveEnd,
    Interval interval

) {

    public static BucketBoundary of(LocalDate inclusiveStart, LocalDate exclusiveEnd, Interval interval) {
        return BucketBoundary.builder()
            .InclusiveStart(inclusiveStart)
            .exclusiveEnd(exclusiveEnd)
            .interval(interval)
            .build();
    }

//    public static List<BucketBoundary> generateBuckets(LocalDate ExclusiveEnd, LocalDate inclusiveStart, Interval interval) {
//
//        List<BucketBoundary> bucket = new ArrayList<>();
//
//        LocalDate cursor = ExclusiveEnd;
//        while (!cursor.isBefore(inclusiveStart.plusDays(1))) {
//            LocalDate currentInclusiveStart = calculatePreviousBoundary(cursor, interval);
//            if (currentInclusiveStart.isBefore(inclusiveStart)) currentInclusiveStart = inclusiveStart;
//            bucket.add(BucketBoundary.of(currentInclusiveStart, cursor, interval));
//            cursor = currentInclusiveStart;
//            if (currentInclusiveStart.equals(inclusiveStart)) break;
//        }
//        return bucket; // 최신 → 과거 역순
//    }

    /**
     * 지정된 기간과 인터벌에 맞는 BucketBoundary 리스트를 생성합니다.
     * 최신 날짜부터 과거 순으로 버킷을 생성하여 반환합니다.
     *
     * @param exclusiveEndDate   기간의 종료일 (포함 X)
     * @param inclusiveStartDate 기간의 시작일 (포함 O)
     * @param interval           버킷의 간격
     * @return 생성된 DateBucket 리스트 (최신 -> 과거 순)
     */
    public static List<BucketBoundary> generateBuckets(LocalDate exclusiveEndDate, LocalDate inclusiveStartDate, Interval interval) {

        List<BucketBoundary> buckets = new ArrayList<>();

        // 커서는 다음 버킷의 exclusiveEndDate가 됨
        LocalDate cursor = exclusiveEndDate.plusDays(1);

        while (cursor.isAfter(inclusiveStartDate)) {

            LocalDate bucketStartDate = calculatePreviousBoundary(cursor, interval);

            // 버킷의 시작일이 전체 기간의 시작일보다 이전이면, 전체 시작일로 맞춰줌
            if (bucketStartDate.isBefore(inclusiveStartDate)) {
                bucketStartDate = inclusiveStartDate;
            }

            buckets.add(BucketBoundary.of(bucketStartDate, cursor, interval));

            // 다음 루프를 위해 커서를 현재 버킷의 시작일로 이동
            cursor = bucketStartDate;
        }

        return Collections.unmodifiableList(buckets);
    }

    /**
     * 차트 표시 기간 외에, 기술적 지표 계산을 위한 이전 데이터(Lookback)를 포함하는 BucketBoundary 리스트를 생성합니다.
     *
     * @param dates              원본 데이터에 포함된 모든 날짜 리스트
     * @param exclusiveEndDate   차트에 표시될 기간의 종료일
     * @param inclusiveStartDate 차트에 표시될 기간의 시작일
     * @param interval           버킷의 간격
     * @return 룩백 기간이 포함된 전체 DateBucket 리스트
     */
    public static List<BucketBoundary> generateBucketsWithLookback(List<LocalDate> dates, LocalDate exclusiveEndDate, LocalDate inclusiveStartDate, Interval interval) {

        // 1. 기본적으로 차트에 표시될 버킷들을 생성
        List<BucketBoundary> displayBuckets = new ArrayList<>(generateBuckets(exclusiveEndDate, inclusiveStartDate, interval));

        // 2. 원본 데이터 중 가장 이른 날짜를 찾음
        Optional<LocalDate> earliestRawDateOpt = dates.stream().min(LocalDate::compareTo);

        // 룩백 데이터가 없거나, 표시 기간보다 더 이른 데이터가 없으면 그대로 반환
        if (earliestRawDateOpt.isEmpty() || !earliestRawDateOpt.get().isBefore(inclusiveStartDate)) {
            return displayBuckets;
        }

        LocalDate earliestRawDate = earliestRawDateOpt.get();

        // 3. 룩백 기간에 해당하는 추가 버킷들을 생성 (displayStartDate 바로 이전 날짜부터 가장 이른 데이터 날짜까지)
        List<BucketBoundary> lookbackBuckets = generateBuckets(inclusiveStartDate.minusDays(1), earliestRawDate, interval);

        // 4. 두 리스트를 합쳐서 반환 (표시용 버킷 + 룩백용 버킷)
        displayBuckets.addAll(lookbackBuckets);
        return Collections.unmodifiableList(displayBuckets);
    }

    /**
     * 주어진 커서(exclusiveEndDate)와 간격을 기준으로 이전 기간의 시작일을 계산합니다.
     */
    private static LocalDate calculatePreviousBoundary(LocalDate cursor, Interval interval) {
        return switch (interval) {
            // 일/주 단위
            case ONE_DAY -> cursor.minusDays(1);
            case ONE_WEEK -> cursor.minusWeeks(1);
            case TWO_WEEK -> cursor.minusWeeks(2);
            // 월,분기,반기,연도 단위 (달력 경계 고정) ──
            case ONE_MONTH -> DateUtil.firstDayOfMonth(cursor.minusDays(1));
            case THREE_MONTH -> DateUtil.firstDayOfQuarter(cursor.minusDays(1));
            case SIX_MONTH -> DateUtil.firstDayOfHalf(cursor.minusDays(1));
            case ONE_YEAR -> DateUtil.firstDayOfYear(cursor.minusDays(1));
        };
    }

}
