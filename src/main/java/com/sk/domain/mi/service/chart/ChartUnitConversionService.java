package com.sk.domain.mi.service.chart;

import com.sk.domain.mi.conversion.UnitConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChartUnitConversionService {

    // Common index for date column
    private static final int IDX_DATE = 0;

    // Candle chart indices
    private static final int IDX_CANDLE_CLOSE = 1;
    private static final int IDX_CANDLE_OPEN = 2;
    private static final int IDX_CANDLE_HIGH = 3;
    private static final int IDX_CANDLE_LOW = 4;

    // Metric chart indices
    private static final int IDX_METRIC_MAIN = 1;
    private static final int IDX_METRIC_REF = 2;

    private final UnitConversionService unitConversionService;

    /**
     * 단위 변환을 수행하고, 변환된 값을 포함한 새로운 데이터 리스트를 반환합니다.
     *
     * @param isCandleChartPossible 캔들 차트 여부
     * @param aggregatedData       집계된 원시 데이터
     * @param selectedColumns      컬럼명 리스트 (date 컬럼이 0번)
     * @param unitConversionNo     단위 변환 번호 (null이거나 빈 데이터면 변환하지 않고 원본 반환)
     * @return 변환된 값이 반영된 데이터 리스트
     */
    public List<Map<String, Object>> convertUnit(boolean isCandleChartPossible, List<Map<String, Object>> aggregatedData, List<String> selectedColumns, Integer unitConversionNo) {

        if (aggregatedData == null || aggregatedData.isEmpty() || unitConversionNo == null) {
            return aggregatedData;
        }

        List<String> columns = selectedColumns;
        String dateCol = columns.get(IDX_DATE);
        int size = aggregatedData.size();

        // 캔들 차트일 경우, open/high/low/close 컬럼만 변환
        if (isCandleChartPossible) {
            String closeCol = columns.get(IDX_CANDLE_CLOSE);
            String openCol = columns.get(IDX_CANDLE_OPEN);
            String highCol = columns.get(IDX_CANDLE_HIGH);
            String lowCol = columns.get(IDX_CANDLE_LOW);

            List<BigDecimal> closeSeries = aggregatedData.stream()
                .map(r -> toBigDecimal(r.get(closeCol)))
                .toList();
            List<BigDecimal> openSeries = aggregatedData.stream()
                .map(r -> toBigDecimal(r.get(openCol)))
                .toList();
            List<BigDecimal> highSeries = aggregatedData.stream()
                .map(r -> toBigDecimal(r.get(highCol)))
                .toList();
            List<BigDecimal> lowSeries = aggregatedData.stream()
                .map(r -> toBigDecimal(r.get(lowCol)))
                .toList();

            List<BigDecimal> convertedClose = unitConversionService.convertTimeSeries(unitConversionNo, closeSeries);
            List<BigDecimal> convertedOpen = unitConversionService.convertTimeSeries(unitConversionNo, openSeries);
            List<BigDecimal> convertedHigh = unitConversionService.convertTimeSeries(unitConversionNo, highSeries);
            List<BigDecimal> convertedLow = unitConversionService.convertTimeSeries(unitConversionNo, lowSeries);

            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put(dateCol, aggregatedData.get(i).get(dateCol));
                row.put(openCol, convertedOpen.get(i));
                row.put(highCol, convertedHigh.get(i));
                row.put(lowCol, convertedLow.get(i));
                row.put(closeCol, convertedClose.get(i));
                result.add(row);
            }
            return result;
        }

        // 메트릭 차트일 경우, 메인과 레퍼런스 컬럼 변환
        String mainCol = columns.get(IDX_METRIC_MAIN);
        List<BigDecimal> mainSeries = aggregatedData.stream()
            .map(r -> toBigDecimal(r.get(mainCol)))
            .toList();
        List<BigDecimal> convertedMain = unitConversionService.convertTimeSeries(unitConversionNo, mainSeries);

        // 결과 생성
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(dateCol, aggregatedData.get(i).get(dateCol));
            row.put(mainCol, convertedMain.get(i));
            // 나머지 ref 컬럼은 원본 값 그대로 복사
            for (int j = IDX_METRIC_REF; j < columns.size(); j++) {
                String col = columns.get(j);
                row.put(col, aggregatedData.get(i).get(col));
            }
            result.add(row);
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
