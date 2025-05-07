package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.RenderAs;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 지정된 기간 간격에 따라 원시 차트 데이터를 시간 버킷으로 집계하는 서비스.
 * 데이터의 마지막 날짜를 기준으로 과거 방향으로 집계합니다.
 */
@Service
public class ChartAggregationService {

    // 버킷 경계를 나타내는 내부 레코드 (시작일 포함, 종료일 미포함)
    private record BucketBoundary(
        LocalDate start,
        LocalDate exclusiveEnd
    ) { }

    /**
     * 원시 데이터(rawData)를 주어진 간격(interval)에 맞춰 집계한다.
     * 데이터의 마지막 날짜를 기준으로 과거 방향으로 버킷을 생성하여 집계한다.
     *
     * @param sortedData    소스에서 조회한 원시 데이터 목록 (시간순 오름차순 정렬 가정)
     * @param interval      집계 단위 (ONE_DAY 제외)
     * @param targetColumns 차트 렌더링에 사용될 컬럼명 리스트 (DATE, OPEN/MAIN, HIGH, LOW, CLOSE, REFERENCE...)
     * @param source        데이터 소스 상세 정보
     * @return 집계된 데이터 포인트 목록 (시간순 오름차순 정렬)
     */
    public List<Map<String, Object>> aggregate(List<Map<String, Object>> sortedData, Interval interval, List<String> targetColumns, GoodsDataSourceDetail source) {

        if (ObjectUtils.isEmpty(sortedData)) {
            return Collections.emptyList();
        }

        // 일간 데이터는 집계 없이 그대로 반환
        if (interval == Interval.ONE_DAY) {
            return sortedData;
        }

        String dateColumn = targetColumns.get(RenderAs.DATE.getIndex());
        boolean isCandleChart = Boolean.TRUE.equals(source.getIsCandleChartPossible());

        // 데이터의 마지막 날짜를 기준으로 과거 방향으로 버킷 경계 계산
        List<BucketBoundary> boundaries = calculateBucketBoundariesBackward(sortedData, interval, dateColumn);

        // 계산된 버킷 경계를 사용하여 데이터 집계
        List<Map<String, Object>> aggregatedData = aggregateUsingBoundaries(sortedData, boundaries, dateColumn, targetColumns, isCandleChart);

        // 최종 결과를 시간순(오름차순)으로 정렬하여 반환
        // aggregateUsingBoundaries 결과가 이미 역순(최신->과거)이므로 뒤집어줌
        Collections.reverse(aggregatedData);
        return aggregatedData;
    }

    /**
     * 데이터의 마지막 날짜부터 과거 방향으로 지정된 간격(interval)에 따라 버킷 경계를 계산한다.
     *
     * @param rows     원시 데이터 목록
     * @param interval 집계 간격
     * @param dateCol  날짜 컬럼명
     * @return 버킷 경계 목록 (역순: 가장 최근 버킷이 첫 번째 요소)
     */
    private List<BucketBoundary> calculateBucketBoundariesBackward(List<Map<String, Object>> rows, Interval interval, String dateCol) {
        List<LocalDate> dates = rows.stream()
            .map(r -> DateUtil.parseDate(r.get(dateCol)))
            .filter(Objects::nonNull)
            .sorted()
            .distinct() // 중복 날짜 제거
            .toList();

        if (dates.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate firstDate = dates.get(0);
        LocalDate lastDate = dates.get(dates.size() - 1);

        List<BucketBoundary> boundaries = new ArrayList<>();
        LocalDate currentEndDate = lastDate.plusDays(1); // 마지막 버킷의 종료일(미포함)

        while (true) {
            LocalDate currentStartDate = retreat(currentEndDate, interval);

            // 현재 계산된 버킷 시작일이 전체 데이터의 첫 날짜보다 이전이거나 같으면,
            // 실제 데이터가 있는 첫 날짜를 이 버킷의 시작일로 조정하고 루프 종료
            if (!currentStartDate.isAfter(firstDate)) {
                boundaries.add(new BucketBoundary(firstDate, currentEndDate));
                break;
            }

            boundaries.add(new BucketBoundary(currentStartDate, currentEndDate));
            currentEndDate = currentStartDate; // 다음 버킷의 종료일(미포함)은 현재 버킷의 시작일

            // 다음 버킷의 시작일이 데이터의 첫 날짜보다 이전이 될 경우 중지
            LocalDate nextStartDate = retreat(currentEndDate, interval);
            if (nextStartDate.isBefore(firstDate)) {
                // 마지막 남은 기간을 위한 버킷 추가 (데이터 첫날 ~ 현재 종료일)
                if (currentEndDate.isAfter(firstDate)) { // 아직 처리 안된 첫 날짜 이후 기간이 있다면
                    boundaries.add(new BucketBoundary(firstDate, currentEndDate));
                }
                break;
            }
        }

        // 생성된 버킷 리스트는 역순 (최신 -> 과거) 상태임
        return boundaries;
    }


    /**
     * 주어진 날짜에서 지정된 간격만큼 과거로 이동시킨다.
     */
    private LocalDate retreat(LocalDate date, Interval interval) {
        return switch (interval) {
            case ONE_WEEK -> date.minusWeeks(1);
            case TWO_WEEK -> date.minusWeeks(2);
            case ONE_MONTH -> date.minusMonths(1);
            case THREE_MONTH -> date.minusMonths(3);
            case SIX_MONTH -> date.minusMonths(6);
            case ONE_YEAR -> date.minusYears(1);
            // ONE_DAY는 aggregate 메소드 초반에 처리되므로 여기선 불필요하나, 완전성을 위해 추가
            case ONE_DAY -> date.minusDays(1);
            // default or throw exception? Assuming valid interval enum
        };
    }

    /**
     * 계산된 버킷 경계에 따라 데이터를 필터링하고 집계한다.
     *
     * @param rows          원시 데이터 목록
     * @param boundaries    버킷 경계 목록 (역순: 최신 버킷 먼저)
     * @param dateCol       날짜 컬럼명
     * @param targetColumns 차트 컬럼명 리스트
     * @param isCandleChart 캔들 차트 여부
     * @return 집계된 데이터 목록 (역순: 최신 버킷 결과가 첫 번째 요소)
     */
    private List<Map<String, Object>> aggregateUsingBoundaries(List<Map<String, Object>> rows, List<BucketBoundary> boundaries, String dateCol, List<String> targetColumns, boolean isCandleChart) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (BucketBoundary boundary : boundaries) {
            // 해당 버킷 기간에 맞는 데이터 필터링 (start <= date < exclusiveEnd)
            List<Map<String, Object>> bucketData = filterByDateRange(rows, dateCol, boundary.start(), boundary.exclusiveEnd());

            if (bucketData.isEmpty()) {
                continue; // 해당 기간에 데이터 없으면 건너뜀
            }

            // 대표 날짜는 버킷의 시작일로 설정
            LocalDate representativeDate = boundary.start();

            Map<String, Object> aggregatedMap = isCandleChart
                ? aggregateCandle(bucketData, representativeDate, dateCol, targetColumns)
                : aggregateLine(bucketData, representativeDate, dateCol, targetColumns);

            if (!aggregatedMap.isEmpty()) {
                result.add(aggregatedMap);
            }
        }
        // 결과는 역순 (최신 버킷 -> 과거 버킷 순서) 상태
        return result;
    }

    /**
     * 주어진 시작일(포함)과 종료일(미포함) 사이의 데이터를 필터링한다.
     */
    private List<Map<String, Object>> filterByDateRange(List<Map<String, Object>> rows, String dateCol, LocalDate start, LocalDate exclusiveEnd) {
        return rows.stream()
            .filter(r -> {
                LocalDate d = DateUtil.parseDate(r.get(dateCol));
                // start <= d < exclusiveEnd
                return d != null && !d.isBefore(start) && d.isBefore(exclusiveEnd);
            })
            .collect(Collectors.toList()); // 결과는 여전히 시간순 정렬 상태 유지
    }

    /**
     * 캔들 차트 데이터 집계 (시가, 종가, 고가, 저가)
     * 대표 날짜는 버킷의 시작일 사용.
     */
    private Map<String, Object> aggregateCandle(List<Map<String, Object>> bucketData, LocalDate representativeDate, String dateCol, List<String> targetColumns) {
        if (bucketData.isEmpty()) return Collections.emptyMap();

        // 버킷 내 데이터는 이미 시간순 정렬되어 있다고 가정
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(dateCol, representativeDate); // 대표 날짜는 버킷 시작일

        // OCHL 컬럼 인덱스 또는 이름 가져오기
        String openCol = targetColumns.get(RenderAs.OPEN.getIndex());
        String highCol = targetColumns.get(RenderAs.HIGH.getIndex());
        String lowCol = targetColumns.get(RenderAs.LOW.getIndex());
        String closeCol = targetColumns.get(RenderAs.CLOSE.getIndex());

        // 시가 (Open): 버킷의 첫 번째 데이터 값
        map.put(openCol, MathUtil.toBigDecimal(bucketData.get(0).get(openCol)));
        // 종가 (Close): 버킷의 마지막 데이터 값
        map.put(closeCol, MathUtil.toBigDecimal(bucketData.get(bucketData.size() - 1).get(closeCol)));
        // 고가 (High): 버킷 내 최대값
        map.put(highCol, max(bucketData, highCol));
        // 저가 (Low): 버킷 내 최소값
        map.put(lowCol, min(bucketData, lowCol));

        return map;
    }

    /**
     * 라인/영역 차트 데이터 집계 (주요 값 및 참조 시리즈 평균)
     * 대표 날짜는 버킷의 시작일 사용.
     */
    private Map<String, Object> aggregateLine(List<Map<String, Object>> bucketData, LocalDate representativeDate, String dateCol, List<String> targetColumns) {
        if (bucketData.isEmpty()) return Collections.emptyMap();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(dateCol, representativeDate); // 대표 날짜는 버킷 시작일

        // 주요 값 (Main): 평균 계산
        String mainCol = targetColumns.get(RenderAs.MAIN.getIndex());
        map.put(mainCol, average(bucketData, mainCol));

        // 참조 시리즈 (Reference): 평균 계산
        for (int i = RenderAs.REFERENCE.getIndex(); i < targetColumns.size(); i++) {
            String refCol = targetColumns.get(i);
            // 참조 컬럼이 실제로 bucketData에 존재하는지 확인하는 로직 추가 고려 가능
            map.put(refCol, average(bucketData, refCol));
        }
        return map;
    }

    // --- 집계 헬퍼 메소드 (min, max, average) ---
    // 기존 메소드 유지 (null 처리 및 스케일 조정 포함)

    private BigDecimal max(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(col)))
            .filter(Objects::nonNull) // null 값 제외
            .max(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO) // 최대값이 없으면 0 (데이터가 없거나 모두 null)
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

    private BigDecimal min(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(col)))
            .filter(Objects::nonNull) // null 값 제외
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO) // 최소값이 없으면 0
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

    private BigDecimal average(List<Map<String, Object>> bucket, String col) {
        List<BigDecimal> values = bucket.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(col)))
            .filter(Objects::nonNull) // null 값은 평균 계산에서 제외
            .toList();

        if (values.isEmpty()) {
            return BigDecimal.ZERO; // 평균 계산할 값이 없으면 0 반환
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, MathUtil::add);
        return MathUtil.divide(sum, BigDecimal.valueOf(values.size())); // null 제외한 개수로 나눔
    }
}
