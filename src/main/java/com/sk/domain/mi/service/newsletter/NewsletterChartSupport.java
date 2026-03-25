package com.sk.domain.mi.service.newsletter;

import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.shared.entity.master.tif.NewsletterChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateDedicatedChart;
import com.sk.domain.shared.enums.chart.RenderAs;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NewsletterChartSupport {

    // y축 구간 수 (tick 수 = Y_AXIS_SECTIONS + 1). 변경 시 이 상수만 수정하면 됨
    private static final int Y_AXIS_SECTIONS = 5;

    public Map<String, Object> toMainChartContent(List<NewsletterChart> flowCharts, LocalDateTime publishDateTime) {
        String[] marketTimes = calculateMarketTimes(publishDateTime);
        List<String> timestamps = new ArrayList<>();
        List<BigDecimal> highValues = new ArrayList<>();
        List<BigDecimal> lowValues = new ArrayList<>();
        List<BigDecimal> openValues = new ArrayList<>();
        List<BigDecimal> closeValues = new ArrayList<>();
        List<BigDecimal> volumeValues = new ArrayList<>();

        if (flowCharts.isEmpty()) {
            return Map.of("aggregationTime", generateAggregationTime(List.of()), "timestamps", List.of(), "series", List.of());
        }

        TreeMap<Long, NewsletterChart> chartMap = new TreeMap<>();
        for (NewsletterChart chart : flowCharts) {
            chartMap.put(Timestamp.valueOf(chart.getTimestamp()).getTime(), chart);
        }

        long startTimestamp = Long.parseLong(marketTimes[2]);
        long endTimestamp = Long.parseLong(marketTimes[3]);
        for (long current = startTimestamp; current <= endTimestamp; current += 5 * 60 * 1000L) {
            timestamps.add(String.valueOf(current));
            NewsletterChart chart = chartMap.get(current);
            highValues.add(chart != null ? chart.getHigh1() : null);
            lowValues.add(chart != null ? chart.getLow1() : null);
            openValues.add(chart != null ? chart.getOpenPrice() : null);
            closeValues.add(chart != null ? chart.getTradePrice1() : null);
            volumeValues.add(chart != null ? chart.getNumMoves() : null);
        }

        // 캔들스틱 y축 범위는 실제 고가(high) 최대 ~ 저가(low) 최소 기준으로 계산
        BigDecimal ohlcMax = highValues.stream().filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal ohlcMin = lowValues.stream().filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        String dtick = calculateAutoDtick(ohlcMax, ohlcMin);
        List<Double> rangeValues = calculateRangeWithDtick(ohlcMax, ohlcMin, dtick);
        String range = String.format("[%s, %s]", rangeValues.get(0), rangeValues.get(1));

        // high/low/open/close는 동일 y축을 공유하므로 같은 dtick/range 적용. volume은 별도 단위
        List<Series> series = Arrays.asList(
            Series.ofWithChartOption("high", "High", RenderAs.HIGH, highValues, true, null, dtick, range, false),
            Series.ofWithChartOption("low", "Low", RenderAs.LOW, lowValues, true, null, dtick, range, false),
            Series.ofWithChartOption("open", "Open", RenderAs.OPEN, openValues, true, null, dtick, range, false),
            Series.ofWithChartOption("close", "Close", RenderAs.CLOSE, closeValues, true, null, dtick, range, false),
            Series.of("volume", "Volume", RenderAs.VOLUME, volumeValues, true)
        );

        return Map.of(
            "aggregationTime", generateAggregationTime(timestamps),
            "timestamps", timestamps,
            "series", series
        );
    }

    public Map<String, Object> toDedicatedChartContent(
        List<NewsletterTemplateDedicatedChart> configs,
        Map<String, List<NewsletterChart>> chartsByCode,
        LocalDateTime publishDateTime
    ) {
        Set<Long> observedTimestamps = new TreeSet<>();
        Map<String, Map<Long, BigDecimal>> valuesBySeriesCode = new HashMap<>();

        for (NewsletterTemplateDedicatedChart config : configs) {
            Map<Long, BigDecimal> valuesByTimestamp = new HashMap<>();
            for (NewsletterChart chart : chartsByCode.getOrDefault(config.getCode(), List.of())) {
                long timestamp = Timestamp.valueOf(chart.getTimestamp()).getTime();
                valuesByTimestamp.put(timestamp, chart.getTradePrice1());
                observedTimestamps.add(timestamp);
            }
            valuesBySeriesCode.put(config.getCode(), valuesByTimestamp);
        }

        if (observedTimestamps.isEmpty()) {
            return Map.of("aggregationTime", generateAggregationTime(List.of()), "timestamps", List.of(), "series", List.of());
        }

        String[] marketTimes = calculateMarketTimes(publishDateTime);
        long chartStartMillis = Long.parseLong(marketTimes[2]);
        long chartEndMillis = Long.parseLong(marketTimes[3]);

        List<Long> axisTimestamps = buildDedicatedTimeAxis(configs, observedTimestamps, chartStartMillis, chartEndMillis);
        axisTimestamps = mergeWithObservedTimestampsInRange(axisTimestamps, observedTimestamps, chartStartMillis, chartEndMillis);
        List<String> timestamps = axisTimestamps.stream().map(String::valueOf).toList();

        List<Series> series = new ArrayList<>();
        for (NewsletterTemplateDedicatedChart config : configs) {
            List<BigDecimal> values = axisTimestamps.stream()
                .map(ts -> valuesBySeriesCode.getOrDefault(config.getCode(), Map.of()).get(ts))
                .toList();

            if (!values.isEmpty()) {
                BigDecimal max = values.stream().filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal min = values.stream().filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                String dtick = calculateAutoDtick(max, min);
                List<Double> rangeValues = calculateRangeWithDtick(max, min, dtick);
                String range = String.format("[%s, %s]", rangeValues.get(0), rangeValues.get(1));
                series.add(Series.ofWithChartOption(
                    config.getColor(),
                    config.getColor(),
                    RenderAs.CLOSE,
                    values,
                    true,
                    config.getColor(),
                    dtick,
                    range,
                    config.isBidirectional()
                ));
            }
        }

        return Map.of(
            "aggregationTime", generateAggregationTime(timestamps),
            "timestamps", timestamps,
            "series", series
        );
    }

    public Map<String, Object> toCollectionChartContent(
        List<NewsletterTemplateCollectionChart> configs,
        NewsletterCollectionChart chart,
        List<CollectionSeriesData> seriesDataList,
        LocalDateTime publishDateTime
    ) {
        String[] marketTimes = calculateMarketTimes(publishDateTime);
        long chartStartMillis = Long.parseLong(marketTimes[2]);
        long chartEndMillis = Long.parseLong(marketTimes[3]);
        Set<Long> unionTimestamps = new TreeSet<>();
        long rangeStartMillis = Math.min(chartStartMillis, chartEndMillis);
        long rangeEndMillis = Math.max(chartStartMillis, chartEndMillis);

        for (CollectionSeriesData seriesData : seriesDataList) {
            unionTimestamps.addAll(seriesData.axisTimestamps());
            unionTimestamps.addAll(seriesData.valuesByTimestamp().keySet().stream()
                .filter(ts -> ts >= rangeStartMillis && ts <= rangeEndMillis)
                .toList());
        }

        if (unionTimestamps.isEmpty()) {
            return Map.of("aggregationTime", generateAggregationTime(List.of()), "timestamps", List.of(), "series", List.of());
        }

        List<Long> timestampList = new ArrayList<>(unionTimestamps);
        List<Series> series = new ArrayList<>();
        for (CollectionSeriesData seriesData : seriesDataList) {
            NewsletterTemplateCollectionChart config = seriesData.config();
            List<BigDecimal> values = timestampList.stream().map(ts -> seriesData.valuesByTimestamp().get(ts)).toList();
            BigDecimal max = values.stream().filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = values.stream().filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            String dtick = calculateAutoDtick(max, min);
            List<Double> rangeValues = calculateRangeWithDtick(max, min, dtick);
            String range = String.format("[%s, %s]", rangeValues.get(0), rangeValues.get(1));
            series.add(Series.ofWithChartOption(
                config.getColor(),
                config.getColor(),
                RenderAs.CLOSE,
                values,
                true,
                config.getColor(),
                dtick,
                range,
                config.isBidirectional()
            ));
        }

        return Map.of(
            "aggregationTime", generateAggregationTime(timestampList.stream().map(String::valueOf).toList()),
            "timestamps", timestampList.stream().map(String::valueOf).toList(),
            "series", series
        );
    }

    public List<Long> buildTimeAxisTimestamps(LocalDateTime startDate, LocalDateTime endDate, BigDecimal intervalValue, String intervalUnit) {
        if (startDate == null || endDate == null || intervalValue == null || StringUtils.isBlank(intervalUnit) || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }
        String normalizedUnit = "WEEEK".equalsIgnoreCase(intervalUnit) ? "WEEK" : intervalUnit.trim().toUpperCase(Locale.ROOT);
        switch (normalizedUnit) {
            case "MINUTE", "HOUR", "DAY", "WEEK" -> {
                long intervalMillis = toIntervalMillis(intervalValue, normalizedUnit);
                if (intervalMillis <= 0) {
                    return Collections.emptyList();
                }
                List<Long> timestamps = new ArrayList<>();
                for (long current = Timestamp.valueOf(startDate).getTime(), end = Timestamp.valueOf(endDate).getTime(); current <= end; current += intervalMillis) {
                    timestamps.add(current);
                }
                return timestamps;
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    public String[] calculateMarketTimes(LocalDateTime publishDateTime) {
        try {
            ZoneId newYorkZone = ZoneId.of("America/New_York");
            LocalDate today = publishDateTime.toLocalDate();
            ZonedDateTime todayInNY = today.atStartOfDay(newYorkZone);
            ZonedDateTime startTime = todayInNY.withHour(9).withMinute(30).withSecond(0).withNano(0);
            ZonedDateTime endTime = todayInNY.withHour(16).withMinute(0).withSecond(0).withNano(0);
            ZonedDateTime chartStartTime = todayInNY.minusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0);
            ZonedDateTime chartEndTime = todayInNY.withHour(17).withMinute(0).withSecond(0).withNano(0);
            return new String[] {
                String.valueOf(startTime.toInstant().toEpochMilli()),
                String.valueOf(endTime.toInstant().toEpochMilli()),
                String.valueOf(chartStartTime.toInstant().toEpochMilli()),
                String.valueOf(chartEndTime.toInstant().toEpochMilli())
            };
        } catch (Exception e) {
            log.error("주식 시장 시간 계산 중 오류 발생", e);
            return new String[] {"1704110200000", "1704133200000", "1704110200000", "1704133200000"};
        }
    }

    private String generateAggregationTime(List<String> timestamps) {
        if (timestamps == null || timestamps.isEmpty()) {
            return "뉴욕시간 전일 20:00부터 당일 17:00까지";
        }
        try {
            ZoneId newYorkZone = ZoneId.of("America/New_York");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String startTime = Instant.ofEpochMilli(Long.parseLong(timestamps.get(0))).atZone(newYorkZone).format(formatter);
            String endTime = Instant.ofEpochMilli(Long.parseLong(timestamps.get(timestamps.size() - 1))).atZone(newYorkZone).format(formatter);
            return "뉴욕시간 전일 " + startTime + "부터 당일 " + endTime + "까지";
        } catch (Exception e) {
            return "뉴욕시간 전일 20:00부터 당일 17:00까지";
        }
    }

    private List<Long> buildDedicatedTimeAxis(List<NewsletterTemplateDedicatedChart> configs, Set<Long> observedTimestamps, long chartStartMillis, long chartEndMillis) {
        Optional<NewsletterTemplateDedicatedChart> axisConfig = configs.stream()
            .filter(config -> config.getTimeAxisIntervalValue() != null && StringUtils.isNotBlank(config.getTimeAxisIntervalUnit()))
            .findFirst();
        if (axisConfig.isEmpty()) {
            return new ArrayList<>(observedTimestamps);
        }
        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(chartStartMillis), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(chartEndMillis), ZoneId.systemDefault());
        List<Long> fixedAxis = buildTimeAxisTimestamps(start, end, axisConfig.get().getTimeAxisIntervalValue(), axisConfig.get().getTimeAxisIntervalUnit());
        return fixedAxis.isEmpty() ? new ArrayList<>(observedTimestamps) : fixedAxis;
    }

    private List<Long> mergeWithObservedTimestampsInRange(List<Long> axisTimestamps, Set<Long> observedTimestamps, long rangeStartMillis, long rangeEndMillis) {
        Set<Long> merged = new TreeSet<>(axisTimestamps);
        observedTimestamps.stream()
            .filter(ts -> ts >= Math.min(rangeStartMillis, rangeEndMillis) && ts <= Math.max(rangeStartMillis, rangeEndMillis))
            .forEach(merged::add);
        return new ArrayList<>(merged);
    }

    private String calculateAutoDtick(BigDecimal max, BigDecimal min) {
        double span = max.subtract(min).abs().doubleValue();
        if (span == 0) {
            span = Math.max(Math.abs(max.doubleValue()), 1.0d);
        }
        double rawStep = span / Y_AXIS_SECTIONS;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double normalized = rawStep / magnitude;
        double niceNormalized = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 2.5 ? 2.5 : normalized <= 5 ? 5 : 10;
        return BigDecimal.valueOf(niceNormalized * magnitude).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private List<Double> calculateRangeWithDtick(BigDecimal max, BigDecimal min, String dtickStr) {
        double dtick = Double.parseDouble(dtickStr);
        double rangeMin = Math.floor(min.doubleValue() / dtick) * dtick;
        double rangeMax = rangeMin + Y_AXIS_SECTIONS * dtick;
        // 부동소수점 오차 등으로 실제 max가 범위를 벗어나는 경우 1 구간 위로 보정
        if (rangeMax < max.doubleValue()) {
            rangeMax += dtick;
        }
        return List.of(rangeMin, rangeMax);
    }

    private long toIntervalMillis(BigDecimal intervalValue, String intervalUnit) {
        long unitMillis = switch (intervalUnit) {
            case "MINUTE" -> 60_000L;
            case "HOUR" -> 3_600_000L;
            case "DAY" -> 86_400_000L;
            case "WEEK" -> 604_800_000L;
            default -> 0L;
        };
        return intervalValue.multiply(BigDecimal.valueOf(unitMillis)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public record CollectionSeriesData(
        NewsletterTemplateCollectionChart config,
        List<Long> axisTimestamps,
        Map<Long, BigDecimal> valuesByTimestamp
    ) {
    }
}
