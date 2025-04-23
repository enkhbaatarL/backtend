package com.sk.domain.mi.service.chart;

import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.RenderAs;
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

    private final UnitConversionService unitConversionService;

    /**
     * 단위 변환을 수행하고, 변환된 값을 포함한 새로운 데이터 리스트를 반환합니다.
     *
     * @param isCandleChartPossible 캔들 차트 여부
     * @param aggregatedData       집계된 원시 데이터
     * @param targetColumns      컬럼명 리스트 (date 컬럼이 0번)
     * @return 변환된 값이 반영된 데이터 리스트
     */
    public List<Map<String, Object>> convert(boolean isCandleChartPossible, List<Map<String, Object>> aggregatedData, List<String> targetColumns, UnitConvert unitConvert) {

        if (aggregatedData == null || aggregatedData.isEmpty() || unitConvert == null) {
            return aggregatedData;
        }

        String dateCol = targetColumns.get(RenderAs.DATE.getIndex());
        int size = aggregatedData.size();

        // 캔들 차트일 경우, open/high/low/close 컬럼만 변환
        if (isCandleChartPossible) {
            String closeCol = targetColumns.get(RenderAs.CLOSE.getIndex());
            String openCol = targetColumns.get(RenderAs.OPEN.getIndex());
            String highCol = targetColumns.get(RenderAs.HIGH.getIndex());
            String lowCol = targetColumns.get(RenderAs.LOW.getIndex());

            List<BigDecimal> closeSeries = aggregatedData.stream()
                .map(r -> MathUtil.toBigDecimal(r.get(closeCol)))
                .toList();
            List<BigDecimal> openSeries = aggregatedData.stream()
                .map(r -> MathUtil.toBigDecimal(r.get(openCol)))
                .toList();
            List<BigDecimal> highSeries = aggregatedData.stream()
                .map(r -> MathUtil.toBigDecimal(r.get(highCol)))
                .toList();
            List<BigDecimal> lowSeries = aggregatedData.stream()
                .map(r -> MathUtil.toBigDecimal(r.get(lowCol)))
                .toList();

            List<BigDecimal> convertedClose = unitConversionService.convertTimeSeries(unitConvert, closeSeries);
            List<BigDecimal> convertedOpen = unitConversionService.convertTimeSeries(unitConvert, openSeries);
            List<BigDecimal> convertedHigh = unitConversionService.convertTimeSeries(unitConvert, highSeries);
            List<BigDecimal> convertedLow = unitConversionService.convertTimeSeries(unitConvert, lowSeries);

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
        String mainCol = targetColumns.get(RenderAs.MAIN.getIndex());
        List<BigDecimal> mainSeries = aggregatedData.stream()
            .map(r -> MathUtil.toBigDecimal(r.get(mainCol)))
            .toList();
        List<BigDecimal> convertedMain = unitConversionService.convertTimeSeries(unitConvert, mainSeries);

        // 결과 생성
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(dateCol, aggregatedData.get(i).get(dateCol));
            row.put(mainCol, convertedMain.get(i));
            // 나머지 ref 컬럼은 원본 값 그대로 복사
            for (int j = RenderAs.REFERENCE.getIndex(); j < targetColumns.size(); j++) {
                String col = targetColumns.get(j);
                row.put(col, aggregatedData.get(i).get(col));
            }
            result.add(row);
        }
        return result;
    }

}
