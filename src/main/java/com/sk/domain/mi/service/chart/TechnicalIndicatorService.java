package com.sk.domain.mi.service.chart;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.dto.response.chart.IndicatorData;
import com.sk.domain.mi.dto.response.chart.SchemaEntry;
import com.sk.domain.shared.enums.ChartRenderType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for generating technical indicators such as Moving Averages and Bollinger Bands.
 */
@Service
public class TechnicalIndicatorService {

    // Period constants
    private static final int PERIOD_SHORT = 5;
    private static final int PERIOD_MEDIUM = 20;
    private static final int PERIOD_LONG = 60;
    private static final int PERIOD_EXTRA_LONG = 120;

    private final UnitConversionService unitConversionService;

    public TechnicalIndicatorService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }


    public List<IndicatorData> generateIndicators(ChartData mainChart) {
        if (mainChart == null) {
            return Collections.emptyList();
        }

        List<BigDecimal> priceSeries = extractMajorMetricSeries(mainChart);
        List<LocalDate> dates = extractDateMetricSeries(mainChart);

        if (priceSeries.isEmpty() || dates.isEmpty()) {
            return Collections.emptyList();
        }

        IndicatorData maData = createMovingAverageData(dates, priceSeries);
        IndicatorData bbData = createBollingerBandData(dates, priceSeries);

        return List.of(maData, bbData);
    }

    private IndicatorData createMovingAverageData(List<LocalDate> dates, List<BigDecimal> series) {
        List<BigDecimal> maShort = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_SHORT);
        List<BigDecimal> maMedium = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_MEDIUM);
        List<BigDecimal> maLong = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_LONG);
        List<BigDecimal> maExtraLong = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_EXTRA_LONG);

        return ChartBundleFactory.createMovingAverageIndicator(dates, maShort, maMedium, maLong, maExtraLong);
    }

    private IndicatorData createBollingerBandData(List<LocalDate> dates, List<BigDecimal> series) {
        List<BollingerBandResult> results = unitConversionService.convertTimeSeriesToBollingerBands(series, PERIOD_MEDIUM);
        return ChartBundleFactory.createBollingerBandsIndicator(dates, results);
    }

    /**
     * ChartData 객체에서 renderAs 'date' 해당하는 시계열 데이터를 추출한다.
     */
    public List<LocalDate> extractDateMetricSeries(ChartData chartData) {

        String dateColumnName = chartData.schema().stream()
            .filter(entry -> ChartRenderType.DATE == entry.renderAs())
            .map(SchemaEntry::name)
            .findFirst()
            .orElseThrow(() -> SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "No 'date' column(renderAs) names found"));

        List<?> rawDataList = chartData.data().get(dateColumnName);

        if (rawDataList == null || rawDataList.isEmpty()) {
            return Collections.emptyList();
        }

        return rawDataList.stream()
            .map(DateUtil::parseDate)
            .collect(Collectors.toList());
    }

    /**
     * ChartData 객체에서 renderAs 'close' 또는 'main' 해당하는 시계열 데이터를 추출한다.
     */
    public List<BigDecimal> extractMajorMetricSeries(ChartData chartData) {

        String majorColumnName = chartData.schema().stream()
            .filter(entry -> ChartRenderType.isMajorType(entry.renderAs()))
            .map(SchemaEntry::name)
            .findFirst()
            .orElseThrow(() -> SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "No major column(major is 'close' or 'main' renderAs) names found"));

        List<?> rawDataList = chartData.data().get(majorColumnName);

        if (rawDataList == null || rawDataList.isEmpty()) {
            return Collections.emptyList();
        }

        return rawDataList.stream()
            .map(s -> new BigDecimal(String.valueOf(s)))
            .collect(Collectors.toList());
    }

}
