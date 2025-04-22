package com.sk.domain.mi.service.chart;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.charts.ChartRes;
import com.sk.domain.mi.dto.response.charts.IndicatorRes;
import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.shared.enums.chart.IndicatorType;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.enums.ChartSettingConditionCd;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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


    public List<IndicatorRes> generateIndicators(com.sk.domain.mi.dto.response.charts.ChartRes chart, Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        if (chart == null) {
            return Collections.emptyList();
        }

        List<BigDecimal> mainSeries = extractMajorMetricSeries(chart);
        List<String> timestamps = chart.timestamps();

        if (mainSeries.isEmpty() || timestamps.isEmpty()) {
            return Collections.emptyList();
        }

        IndicatorRes maData = createMovingAverageData(timestamps, mainSeries, personalizedMap);
        IndicatorRes bbData = createBollingerBandData(timestamps, mainSeries, personalizedMap);

        return List.of(maData, bbData);
    }

    private IndicatorRes createMovingAverageData(List<String> timestamps, List<BigDecimal> series, Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<BigDecimal> maShort = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_SHORT);
        List<BigDecimal> maMedium = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_MEDIUM);
        List<BigDecimal> maLong = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_LONG);
        List<BigDecimal> maExtraLong = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_EXTRA_LONG);

        List<Series> movingAverageIndicator = ChartBundleFactory.createMovingAverageIndicator(maShort, maMedium, maLong, maExtraLong, personalizedMap);

        return IndicatorRes.of(IndicatorType.MOVING_AVERAGE, timestamps, movingAverageIndicator);
    }

    private IndicatorRes createBollingerBandData(List<String> timestamps, List<BigDecimal> series, Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<BollingerBandResult> results = unitConversionService.convertTimeSeriesToBollingerBands(series, PERIOD_MEDIUM);

        List<Series> bollingerBandsIndicator = ChartBundleFactory.createBollingerBandsIndicator(results, personalizedMap);

        return IndicatorRes.of(IndicatorType.BOLLINGER_BANDS, timestamps, bollingerBandsIndicator);
    }

    /**
     * ChartData 객체에서 renderAs 'close' 또는 'main' 해당하는 시계열 데이터를 추출한다.
     */
    public List<BigDecimal> extractMajorMetricSeries(ChartRes chart) {

        Series series = chart.series().stream()
            .filter(entry -> RenderAs.isMajorType(entry.renderAs()))
            .findFirst().orElseThrow(() -> SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "No major column(major is 'close' or 'main' renderAs) names found"));

        if (series == null || series.values().isEmpty()) {
            return Collections.emptyList();
        }

        return series.values();
    }

}
