package com.sk.domain.mi.service.chart;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.charts.ChartRes;
import com.sk.domain.mi.dto.response.charts.IndicatorRes;
import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.shared.enums.chart.IndicatorType;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 이동평균선, 볼린저 밴드와 같은 기술적 지표를 생성하는 서비스
 * 차트 데이터를 기반으로 다양한 기술적 지표를 계산하고 생성한다.
 */
@Service
@RequiredArgsConstructor
public class TechnicalIndicatorService {

    // Period constants
    private static final int PERIOD_SHORT = 5;
    private static final int PERIOD_MEDIUM = 20;
    private static final int PERIOD_LONG = 60;
    private static final int PERIOD_EXTRA_LONG = 120;

    private final UnitConversionService unitConversionService;
    private final PersonalizationService personalizationService;


    /**
     * 차트 데이터를 기반으로 기술적 지표를 생성한다.
     * 이동평균선과 볼린저 밴드 지표를 계산하고, 개인화 설정에 따라 표시 여부를 결정한다.
     * 차트 데이터가 없거나 시계열 데이터가 비어있는 경우 빈 리스트를 반환한다.
     *
     * @param chart 기술적 지표를 생성할 차트 데이터
     * @param personalizedMap 개인화 설정 맵
     * @return 생성된 기술적 지표 목록
     */
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

    /**
     * 이동평균선 지표 데이터를 생성한다.
     * 시계열 데이터를 기반으로 5일, 20일, 60일, 120일 이동평균선을 계산하고,
     * 개인화 설정에 따라 표시 여부를 결정하여 지표 데이터를 생성한다.
     *
     * @param timestamps 시간 데이터 목록
     * @param series 시계열 데이터 목록
     * @param personalizedMap 개인화 설정 맵
     * @return 이동평균선 지표 데이터
     */
    private IndicatorRes createMovingAverageData(List<String> timestamps, List<BigDecimal> series, Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<BigDecimal> maShort = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_SHORT);
        List<BigDecimal> maMedium = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_MEDIUM);
        List<BigDecimal> maLong = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_LONG);
        List<BigDecimal> maExtraLong = unitConversionService.convertTimeSeriesToMovingAverage(series, PERIOD_EXTRA_LONG);

        // Get personalized indicators using the PersonalizationService
        List<String> personalizedIndicators = personalizationService.getPersonalizedIndicators(personalizedMap);

        List<Series> movingAverageIndicator = ChartBundleFactory.createMovingAverageIndicator(maShort, maMedium, maLong, maExtraLong, personalizedIndicators);

        return IndicatorRes.of(IndicatorType.MOVING_AVERAGE, timestamps, movingAverageIndicator);
    }

    /**
     * 볼린저 밴드 지표 데이터를 생성한다.
     * 시계열 데이터를 기반으로 볼린저 밴드(상단, 중간, 하단)를 계산하고,
     * 개인화 설정에 따라 표시 여부를 결정하여 지표 데이터를 생성한다.
     *
     * @param timestamps 시간 데이터 목록
     * @param series 시계열 데이터 목록
     * @param personalizedMap 개인화 설정 맵
     * @return 볼린저 밴드 지표 데이터
     */
    private IndicatorRes createBollingerBandData(List<String> timestamps, List<BigDecimal> series, Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<BollingerBandResult> results = unitConversionService.convertTimeSeriesToBollingerBands(series, PERIOD_MEDIUM);

        // Get personalized indicators using the PersonalizationService
        List<String> personalizedIndicators = personalizationService.getPersonalizedIndicators(personalizedMap);

        List<Series> bollingerBandsIndicator = ChartBundleFactory.createBollingerBandsIndicator(results, personalizedIndicators);

        return IndicatorRes.of(IndicatorType.BOLLINGER_BANDS, timestamps, bollingerBandsIndicator);
    }

    /**
     * ChartData 객체에서 renderAs 'close' 또는 'main'에 해당하는 시계열 데이터를 추출한다.
     * 차트 데이터에서 주요 시계열 데이터(종가 또는 메인 데이터)를 찾아 반환한다.
     * 주요 시계열 데이터가 없는 경우 예외를 발생시킨다.
     *
     * @param chart 시계열 데이터를 추출할 차트 데이터
     * @return 추출된 시계열 데이터 목록
     * @throws SystemErrorException 주요 시계열 데이터를 찾지 못한 경우
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
