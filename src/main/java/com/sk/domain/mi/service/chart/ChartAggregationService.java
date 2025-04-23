package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.RenderAs;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 지정된 기간 간격에 따라 원시 차트 데이터를 시간 버킷으로 집계하는 서비스
 */
@Service
public class ChartAggregationService {

    /**
     * 원시 데이터(rawData)를 주어진 간격(currentInterval)과 소스 정보(source)에 맞춰 집계한다.
     *
     * @param sortedData 소스에서 조회한 원시 데이터 목록
     * @param interval   집계 단위
     * @param source     데이터 소스 상세 정보
     * @return 집계된 데이터 포인트 목록
     */
    public List<Map<String, Object>> aggregate(List<Map<String, Object>> sortedData, Interval interval, List<String> targetColumns, GoodsDataSourceDetail source) {

        if (sortedData == null || sortedData.isEmpty() || source == null) {
            return Collections.emptyList();
        }

        String xAxis = targetColumns.get(RenderAs.DATE.getIndex());
        boolean isCandle = Boolean.TRUE.equals(source.getIsCandleChartPossible());

        // 일간 데이터는 그대로 반환
        if (interval == Interval.ONE_DAY) {
            return sortedData;
        }

        // 버킷 경계 계산 및 집계
        List<LocalDate> bucketStarts = calculateBucketStarts(sortedData, interval, xAxis);

        return aggregateByBuckets(sortedData, bucketStarts, xAxis, targetColumns, isCandle);
    }

    private List<LocalDate> calculateBucketStarts(List<Map<String, Object>> rows, Interval interval, String dateCol) {

        List<LocalDate> dates = rows.stream()
            .map(r -> DateUtil.parseDate(r.get(dateCol)))
            .filter(Objects::nonNull)
            .sorted()
            .toList();

        if (dates.isEmpty()) return Collections.emptyList();

        LocalDate first = dates.get(0);
        LocalDate last = dates.get(dates.size() - 1);
        List<LocalDate> buckets = new ArrayList<>();
        buckets.add(first);

        LocalDate cursor = first;
        while (cursor.isBefore(last)) {
            cursor = advance(cursor, interval);
            buckets.add(cursor);
        }
        return buckets;
    }

    private LocalDate advance(LocalDate date, Interval interval) {
        return switch (interval) {
            case ONE_WEEK -> date.plusWeeks(1);
            case TWO_WEEK -> date.plusWeeks(2);
            case ONE_MONTH -> date.plusMonths(1);
            case THREE_MONTH -> date.plusMonths(3);
            case SIX_MONTH -> date.plusMonths(6);
            case ONE_YEAR -> date.plusYears(1);
            case ONE_DAY -> date.plusDays(1);
        };
    }

    private List<Map<String, Object>> aggregateByBuckets(List<Map<String, Object>> rows, List<LocalDate> buckets, String dateCol, List<String> targetColumns, boolean isCandle) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < buckets.size() - 1; i++) {
            LocalDate start = buckets.get(i);
            LocalDate end = buckets.get(i + 1);
            List<Map<String, Object>> bucket = filterByDate(rows, dateCol, start, end);
            if (bucket.isEmpty()) continue;

            Map<String, Object> agg = isCandle ? aggregateCandle(bucket, dateCol, targetColumns) : aggregateMetric(bucket, dateCol, targetColumns);
            if (!agg.isEmpty()) result.add(agg);
        }
        return result;
    }

    private List<Map<String, Object>> filterByDate(List<Map<String, Object>> rows, String dateCol, LocalDate start, LocalDate end) {
        return rows.stream()
            .filter(r -> {
                LocalDate d = DateUtil.parseDate(r.get(dateCol));
                return d != null && !d.isBefore(start) && !d.isAfter(end);
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> aggregateCandle(List<Map<String, Object>> bucket, String dateCol, List<String> targetColumns) {
        Map<String, Object> map = new LinkedHashMap<>();
        LocalDate date = DateUtil.parseDate(bucket.get(0).get(dateCol));
        map.put(dateCol, DateUtil.toYmd(date)); // todo 여깄었네...
        map.put(targetColumns.get(RenderAs.OPEN.getIndex()), MathUtil.toBigDecimal(bucket.get(0).get(targetColumns.get(RenderAs.OPEN.getIndex()))));
        map.put(targetColumns.get(RenderAs.CLOSE.getIndex()), MathUtil.toBigDecimal(bucket.get(bucket.size() - 1).get(targetColumns.get(RenderAs.OPEN.getIndex()))));
        map.put(targetColumns.get(RenderAs.HIGH.getIndex()), max(bucket, targetColumns.get(RenderAs.HIGH.getIndex())));
        map.put(targetColumns.get(RenderAs.LOW.getIndex()), min(bucket, targetColumns.get(RenderAs.LOW.getIndex())));
        return map;
    }

    private Map<String, Object> aggregateMetric(List<Map<String, Object>> bucket, String dateCol, List<String> targetColumns) {
        Map<String, Object> map = new LinkedHashMap<>();
        LocalDate date = DateUtil.parseDate(bucket.get(0).get(dateCol));
        map.put(dateCol, DateUtil.toYmd(date)); // todo 여깄었네...
        // main value
        map.put(targetColumns.get(RenderAs.MAIN.getIndex()), average(bucket, targetColumns.get(RenderAs.MAIN.getIndex())));
        // reference series
        for (int i = RenderAs.REFERENCE.getIndex(); i < targetColumns.size(); i++) {
            map.put(targetColumns.get(RenderAs.REFERENCE.getIndex()), average(bucket, targetColumns.get(RenderAs.REFERENCE.getIndex())));
        }
        return map;
    }

    private BigDecimal max(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(col)))
            .max(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO)
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

    private BigDecimal min(List<Map<String, Object>> bucket, String col) {
        return bucket.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(col)))
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO)
            .setScale(MathUtil.GLOBAL_SCALE, MathUtil.GLOBAL_ROUNDING);
    }

    private BigDecimal average(List<Map<String, Object>> bucket, String col) {
        BigDecimal sum = bucket.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(col)))
            .reduce(BigDecimal.ZERO, MathUtil::add);

        return MathUtil.divide(sum, BigDecimal.valueOf(bucket.size()));
    }

}
