package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.service.chart.dto.BucketBoundary;
import com.sk.domain.mi.service.chart.dto.ChartSettingsContext;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.RenderAs;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartAggregationService {

    /**
     * 지정된 인터벌과 설정에 따라 정렬된 데이터를 집계합니다.
     *
     * @param sortedData    처리할 정렬된 데이터 맵 리스트
     * @param ctx           인터벌, 시작일자, 종료일자 설정이 포함된 차트 설정 컨텍스트
     * @param targetColumns 집계 시 고려할 대상 컬럼명 리스트
     * @param gds           상품 데이터소스 상세
     * @return 지정된 인터벌과 경계값에 따라 집계된 데이터 맵 리스트
     */
    public List<Map<String, Object>> aggregate(List<Map<String, Object>> sortedData, ChartSettingsContext ctx, List<String> targetColumns, GoodsDataSourceDetail gds) {

        if (Interval.ONE_DAY == ctx.finalizedInterval()) {
            return sortedData;
        }

        String dateCol = targetColumns.get(RenderAs.DATE.getIndex());

        List<LocalDate> dates = sortedData.stream()
            .map(r -> DateUtil.parseDate(r.get(dateCol)))
            .filter(Objects::nonNull)
            .sorted()
            .distinct() // 중복 날짜 제거
            .toList();


        Interval interval = ctx.finalizedInterval();
        LocalDate inclusiveStart = Interval.resolveInclusiveStart(ctx.finalizedInterval(), ctx.finalizedStartDate());
        LocalDate exclusiveEnd = ctx.finalizedEndDate().plusDays(1);

        // 버킷 생성
        List<BucketBoundary> boundaries = BucketBoundary.generateBucketsWithLookback(dates, exclusiveEnd, inclusiveStart, interval);

        return aggregate(sortedData, boundaries, targetColumns, gds);
    }

    public List<Map<String, Object>> aggregate(List<Map<String, Object>> sortedData, List<BucketBoundary> boundaries, List<String> targetColumns, GoodsDataSourceDetail gds) {

        if (ObjectUtils.isEmpty(sortedData)) {
            return Collections.emptyList();
        }

        String dateColumn = targetColumns.get(RenderAs.DATE.getIndex());
        String volumeColumn = gds.getTradeVolumeColumnNm();
        boolean isCandle = Boolean.TRUE.equals(gds.getIsCandleChartPossible());

        List<Map<String, Object>> aggregated = aggregateUsingBoundaries(sortedData, boundaries, dateColumn, volumeColumn, targetColumns, isCandle);

        // asc 정렬
        Collections.reverse(aggregated);

        return aggregated;
    }

    private List<Map<String, Object>> aggregateUsingBoundaries(List<Map<String, Object>> rows, List<BucketBoundary> boundaries, String dateCol, String volumeCol, List<String> targetColumns, boolean isCandle) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (BucketBoundary b : boundaries) {

            List<Map<String, Object>> bucket = filterByDateRange(rows, dateCol, b.InclusiveStart(), b.exclusiveEnd());

            if (bucket.isEmpty()) {
                result.add(emptyRow(b.InclusiveStart(), dateCol, volumeCol, targetColumns));
                continue;
            }

            // 시작일 -> 대표일로 선정
            LocalDate representative = b.InclusiveStart();

            Map<String, Object> aggregateData = isCandle
                ? aggregateCandle(bucket, representative, dateCol, volumeCol, targetColumns)
                : aggregateLine(bucket, representative, dateCol, volumeCol, targetColumns);

            result.add(aggregateData);
        }

        return result; // 최신→과거 역순 (호출부에서 reverse)
    }

    private Map<String, Object> emptyRow(LocalDate date, String dateCol, String volCol, List<String> cols) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(dateCol, date);
        for (String c : cols) {
            if (!c.equals(dateCol)) m.put(c, null);
        }
        if (StringUtils.isNotEmpty(volCol)) m.put(volCol, null);
        return m;
    }

    private List<Map<String, Object>> filterByDateRange(List<Map<String, Object>> rows, String dateCol, LocalDate start, LocalDate exclusiveEnd) {
        return rows.stream()
            .filter(r -> {
                LocalDate d = DateUtil.parseDate(r.get(dateCol));
                return d != null && isBetween(d, start, exclusiveEnd);
            })
            .collect(Collectors.toList());
    }

    private static boolean isBetween(LocalDate d, LocalDate start, LocalDate exclusiveEnd) {
        return (d.isEqual(start) || d.isAfter(start)) && d.isBefore(exclusiveEnd);
    }

    private Map<String, Object> aggregateCandle(List<Map<String, Object>> bucket, LocalDate representative, String dateCol, String volumeCol, List<String> targetCols) {

        if (bucket.isEmpty()) return Collections.emptyMap();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put(dateCol, representative);

        String openCol = targetCols.get(RenderAs.OPEN.getIndex());
        String highCol = targetCols.get(RenderAs.HIGH.getIndex());
        String lowCol = targetCols.get(RenderAs.LOW.getIndex());
        String closeCol = targetCols.get(RenderAs.CLOSE.getIndex());

        m.put(openCol, avg(bucket, openCol));
        m.put(highCol, avg(bucket, highCol));
        m.put(lowCol, avg(bucket, lowCol));
        m.put(closeCol, avg(bucket, closeCol));

        // 거래량 컬럼이 있는 경우
        if (StringUtils.isNotEmpty(volumeCol)) m.put(volumeCol, sum(bucket, volumeCol));

        return m;
    }

    private Map<String, Object> aggregateLine(List<Map<String, Object>> bucket, LocalDate representative, String dateCol, String volumeCol, List<String> targetCols) {

        if (bucket.isEmpty()) return Collections.emptyMap();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put(dateCol, representative);

        String main = targetCols.get(RenderAs.MAIN.getIndex());
        m.put(main, avg(bucket, main));

        for (int i = RenderAs.REFERENCE.getIndex(); i < targetCols.size(); i++) {
            String col = targetCols.get(i);
            m.put(col, avg(bucket, col));
        }

        // 거래량 컬럼이 있는 경우
        if (StringUtils.isNotEmpty(volumeCol)) m.put(volumeCol, sum(bucket, volumeCol));

        return m;
    }

    private BigDecimal avg(List<Map<String, Object>> bucket, String col) {
        List<BigDecimal> values = bucket.stream()
            .filter(s -> ObjectUtils.isNotEmpty(s.get(col))) // null 계산에서 제외
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(col)))
            .toList();

        if (values.isEmpty()) {
            return BigDecimal.ZERO; // 평균 계산할 값이 없으면 0 반환
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, MathUtil::add);
        return MathUtil.divide(sum, BigDecimal.valueOf(values.size())); // null 제외한 개수로 나눔
    }

    private BigDecimal sum(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .filter(s -> ObjectUtils.isNotEmpty(s.get(col))) // null 계산에서 제외
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(col)))
            .reduce(BigDecimal.ZERO, MathUtil::add)
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

    private BigDecimal max(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .filter(s -> ObjectUtils.isNotEmpty(s.get(col))) // null 계산에서 제외
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(col)))
            .max(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO) // 최대값이 없으면 0 (데이터가 없거나 모두 null)
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

    private BigDecimal min(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .filter(s -> ObjectUtils.isNotEmpty(s.get(col))) // null 계산에서 제외
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(col)))
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO) // 최소값이 없으면 0
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

}
