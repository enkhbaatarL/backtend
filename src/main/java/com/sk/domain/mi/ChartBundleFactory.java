package com.sk.domain.mi;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.dto.response.chart.IndicatorData;
import com.sk.domain.mi.dto.response.chart.SchemaEntry;
import com.sk.domain.shared.enums.ChartRenderType;
import com.sk.domain.shared.enums.ChartType;
import com.sk.domain.shared.enums.IndicatorType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChartBundleFactory {

    /**
     * ChartData 객체에서 'close' 또는 'main' 해당하는 데이터 자표 시리즈를 추출한다.
     */
    public static List<BigDecimal> extractMajorMetricSeries(ChartData chartData) {

        String majorColumnName = chartData.schema().stream()
            .filter(entry -> ChartRenderType.isMajorType(entry.renderAs()))
            .map(SchemaEntry::name)
            .findFirst()
            .orElseThrow(() -> SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "No major column names found"));

        List<?> rawDataList = chartData.data().get(majorColumnName);

        if (rawDataList == null || rawDataList.isEmpty()) {
            return Collections.emptyList();
        }

        return rawDataList.stream()
            .map(s -> new BigDecimal(String.valueOf(s)))
            .collect(Collectors.toList());
    }

    public static ChartData convertToChartData(boolean isCandleChartPossible, List<Map<String, Object>> rawData, String tableColumnListNm, Map<String, String> columnLabelsMap) {

        // todo 파라미터 검증...

        List<String> columnNamesInOrder = Arrays.stream(tableColumnListNm.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        List<SchemaEntry> schema = IntStream.range(0, columnNamesInOrder.size())
            .mapToObj(i -> {
                String dbColName = columnNamesInOrder.get(i);
                ChartRenderType renderAs = determineRenderAsByIndex(isCandleChartPossible, i);
                String label = columnLabelsMap.getOrDefault(dbColName, dbColName);
                return new SchemaEntry(dbColName, renderAs, label);
            })
            .collect(Collectors.toList());

        Map<String, List<?>> data = new HashMap<>();
        for (String dbColName : columnNamesInOrder) {
            List<Object> columnValues = rawData.stream()
                .map(row -> {
                    Object value = row.get(dbColName);

                    // 첫 번째 컬럼(날짜)이면 포맷 변환
                    if (dbColName.equalsIgnoreCase(columnNamesInOrder.get(0))) {
                        if (value instanceof LocalDateTime ldt) {
                            return ldt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        } else if (value instanceof Timestamp ts) {
                            return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        } else if (value instanceof String str) {
                            try {
                                LocalDateTime ldt = LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                return ldt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                            } catch (Exception e) {
                                return str;
                            }
                        }
                    }

                    return value;
                })
                .collect(Collectors.toList());

            data.put(dbColName, columnValues);
        }

        return new ChartData(isCandleChartPossible ? ChartType.CANDLE.name() : ChartType.METRIC.name(), columnNamesInOrder, schema, data);
    }

    /**
     * 컬럼 인덱스를 기반으로 renderAs 값을 결정합니다.
     */
    private static ChartRenderType determineRenderAsByIndex(boolean isCandleChartPossible, int index) {
        ChartType chartType = isCandleChartPossible ? ChartType.CANDLE : ChartType.METRIC;
        return chartType.resolveRenderType(index);
    }

    /**
     * 이동평균선(MA) 지표 데이터를 생성하는 메서드
     */
    public static IndicatorData createMovingAverageIndicator(ChartData chartData, List<BigDecimal> ma5, List<BigDecimal> ma20, List<BigDecimal> ma60, List<BigDecimal> ma120) {

        // 날짜 시계열 데이터 추출
        List<?> dateData = chartData.data().get(chartData.columns().get(0));

        // 컬럼명 목록 생성
        List<String> columns = List.of("date", "ma5", "ma20", "ma60", "ma120");

        // 스키마 생성
        List<SchemaEntry> schema = List.of(
            new SchemaEntry("date", ChartRenderType.DATE, "날짜"),
            new SchemaEntry("ma5", ChartRenderType.MA, "5일 이동평균"),
            new SchemaEntry("ma20", ChartRenderType.MA, "20일 이동평균"),
            new SchemaEntry("ma60", ChartRenderType.MA, "60일 이동평균"),
            new SchemaEntry("ma120", ChartRenderType.MA, "120일 이동평균")
        );

        // 데이터 맵 생성
        Map<String, List<?>> data = Map.of(
            "date", dateData,
            "ma5", ma5,
            "ma20", ma20,
            "ma60", ma60,
            "ma120", ma120
        );

        // IndicatorData 생성 및 반환
        return new IndicatorData(IndicatorType.MOVING_AVERAGE.name(), columns, schema, data);
    }

    /**
     * 볼린저 밴드(BB) 지표 데이터를 생성하는 메서드
     */
    public static IndicatorData createBollingerBandsIndicator(ChartData chartData, List<BollingerBandResult> bollingerBandResults) {

        // 날짜 시계열 데이터 추출
        List<?> dateData = chartData.data().get(chartData.columns().get(0));

        // 볼린저 밴드 데이터 추출
        List<BigDecimal> middleBand = new ArrayList<>();
        List<BigDecimal> upperBand = new ArrayList<>();
        List<BigDecimal> lowerBand = new ArrayList<>();

        for (BollingerBandResult result : bollingerBandResults) {
            middleBand.add(result.middle());
            upperBand.add(result.upper());
            lowerBand.add(result.lower());
        }

        // 컬럼명 목록 생성
        List<String> columns = List.of("date", "middle", "upper", "lower");

        // 스키마 생성
        List<SchemaEntry> schema = new ArrayList<>();
        schema.add(new SchemaEntry("date", ChartRenderType.BB, "날짜"));
        schema.add(new SchemaEntry("middle", ChartRenderType.BB, "중앙선"));
        schema.add(new SchemaEntry("upper", ChartRenderType.BB, "상단 밴드"));
        schema.add(new SchemaEntry("lower", ChartRenderType.BB, "하단 밴드"));

        // 데이터 맵 생성
        // 데이터 맵 생성
        Map<String, List<?>> data = Map.of(
            "date", dateData,
            "upper", upperBand,
            "middle", middleBand,
            "lower", lowerBand
        );

        // IndicatorData 생성 및 반환
        return new IndicatorData(IndicatorType.BOLLINGER_BANDS.name(), columns, schema, data);
    }

}
