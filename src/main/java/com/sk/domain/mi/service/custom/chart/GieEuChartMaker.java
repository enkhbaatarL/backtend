package com.sk.domain.mi.service.custom.chart;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.model.ValidationResult;
import com.sk.common.utils.ChartSliceUtil;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.charts.Chart;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.charts.ChartMetadata;
import com.sk.domain.mi.dto.response.charts.Indicator;
import com.sk.domain.mi.service.MiDataDetailService;
import com.sk.domain.mi.service.chart.ChartBundleFactory;
import com.sk.domain.mi.service.chart.ChartGenerationService;
import com.sk.domain.mi.service.chart.PersonalizationService;
import com.sk.domain.mi.service.chart.TechnicalIndicatorService;
import com.sk.domain.mi.service.chart.dto.ChartSettingsContext;
import com.sk.domain.mi.service.chart.dto.UnitCd;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GieEuChartMaker implements CustomChartMaker<ChartBundleRes>{

    private final GoodsDataSourceValidator validatedGoodsDataSource;
    private final MiDataDetailService miDataDetailService;
    private final ChartGenerationService chartGenerationService;
    private final TechnicalIndicatorService technicalIndicatorService;

    @Value("${custom.gie-eu.goods-data-source-no}")
    private Integer goodsDataSourceNo;


    @Override
    public String getCustomId() {
        return "gie-eu";
    }

    @Override
    public ChartBundleRes getDashboardChart() {

        // TODO 사용자의 데이터 접근 권한이 있는지 체크 후 처리 해야 함.

        ValidationResult<GoodsDataSourceDetail> validationResult = validatedGoodsDataSource.ensureExists(goodsDataSourceNo);
        GoodsDataSourceDetail goodsDataSourceDetail = validationResult.data();

        // 요청 파라미터 1차 유효성 hard 검증 -> 검증 실패 시, 예외 처리
        ChartSettingsContext settingsContextV1 = miDataDetailService.hardValidateChartParameters(goodsDataSourceDetail, null, null, null, null, null);
        ChartSettingsContext settingsContextV2 = setDefault(goodsDataSourceDetail, settingsContextV1);
        ChartSettingsContext settingsContextV3 = miDataDetailService.softValidateChartParametersAndAdjust(goodsDataSourceDetail, settingsContextV2);

        // 차트 데이터 생성
        ChartBundleRes bundle = generateChartBundle(goodsDataSourceDetail, settingsContextV3);

        // 차트 데이터 슬라이싱 및 반환
        return ChartSliceUtil.sliceBundle(bundle, DateUtil.parseDate(settingsContextV3.finalizedRangeBetween().getLeft()), settingsContextV3.finalizedInterval());

    }

    private ChartSettingsContext setDefault(GoodsDataSourceDetail gds, ChartSettingsContext contextV1) {

        Interval finalizedInterval = gds.getPeriodUnitSeparateCd().minInterval();
        Range finalizedRange = gds.getPeriodUnitSeparateCd().rangeInterval();

        // RangeBetween 결정: 요청값 > 개인화 > 기본값
        Pair<String, String> finalizedRangeBetween = DateUtil.getDateRange(gds.getPeriodUnitSeparateCd().rangeInterval());

        // UnitCd 결정: 요청값 > 개인화 > 기본값
        UnitCd finalizedUnitCd = new UnitCd(gds.getConvertTargetDetailTypeNm(), gds.getGoodsDataSourceUnitNm());

        // ChartType 결정: 개인화 > 기본값
        ChartType finalizedChartType = ChartType.LINE;

        // Indicators 결정: 개인화
        List<String> finalizedIndicators = Collections.emptyList();

        // ComparisonChartIds 결정: 개인화
        List<Integer> finalizedCompIds = Collections.emptyList();

        // settingsContextV1 -> settingsContextV2
        return contextV1
            .withFinalizedInterval(finalizedInterval)
            .withFinalizedRange(finalizedRange)
            .withFinalizedRangeBetween(finalizedRangeBetween)
            .withFinalizedUnitCd(finalizedUnitCd)
            .withFinalizedChartType(finalizedChartType)
            .withFinalizedIndicators(finalizedIndicators)
            .withFinalizedCompIds(finalizedCompIds);
    }

    private ChartBundleRes generateChartBundle(GoodsDataSourceDetail gds, ChartSettingsContext contextV3) {

        DataSourceContextHolder.setDataSourceKey(gds.getTargetDbNm());
        LocalDate latestDate = chartGenerationService.getLatestDate(gds);

        // [mainChart] 생성
        Chart mainChart = chartGenerationService.generateChartData(gds, contextV3, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);

        // [indicators] 생성
        List<Indicator> indicators = technicalIndicatorService.generateIndicators(mainChart, contextV3.finalizedIndicators());

        // [metadata] 생성
        ChartMetadata metadata = ChartMetadata.of(gds, contextV3, DateUtil.toYmd(latestDate), Boolean.FALSE);

        return ChartBundleFactory.create(metadata, mainChart, indicators, Collections.emptyList());
    }
}
