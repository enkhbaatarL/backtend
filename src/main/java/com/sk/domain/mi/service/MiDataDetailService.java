package com.sk.domain.mi.service;

import com.sk.common.model.ValidationResult;
import com.sk.domain.mi.dto.response.chart.ChartBundleRes;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.dto.response.chart.ChartMetadata;
import com.sk.domain.mi.dto.response.chart.IndicatorData;
import com.sk.domain.mi.service.chart.ChartBundleFactory;
import com.sk.domain.mi.service.chart.ChartGenerationService;
import com.sk.domain.mi.service.chart.TechnicalIndicatorService;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.Interval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final GoodsDataSourceValidator goodsDataSourceDetailValidator;

    private final ChartGenerationService chartGenerationService;
    private final TechnicalIndicatorService technicalIndicatorService;

    /**
     * 상품 데이터소스 차트 묶음을 조회한다.
     *
     * @param goodsDataNo 상품 데이터 번호
     * @param goodsDataSourceNo 상품 데이터소스 번호
     * @param interval 인터벌
     * @param startDate 범위 시작일
     * @param endDate 범위 종료일
     * @return ChartBundleRes
     */
    public ChartBundleRes getDynamicCharts(Integer goodsDataNo, Integer goodsDataSourceNo, Interval interval, String startDate, String endDate) {

        // todo 1. 단위 변환
        // todo 2. 개인화(개인화에 따라 보조지표, 비교군 view 여부)

        goodsDataValidator.ensureExists(goodsDataNo).validOrThrow();

        ValidationResult<GoodsDataSourceDetail> goodsDataSourceResult = goodsDataSourceDetailValidator.ensureExists(goodsDataSourceNo);
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceResult.getOrThrow();

        ChartMetadata metadata = ChartMetadata.of(goodsDataSourceDetail.getGoodsDataSourceUnitNm(), interval);
        ChartData mainChart = chartGenerationService.generateChartData(goodsDataSourceDetail, startDate, endDate, interval);
        List<IndicatorData> indicators = technicalIndicatorService.generateIndicators(mainChart);
        List<ChartData> comparisonCharts = chartGenerationService.generateComparisonCharts(goodsDataSourceDetail, startDate, endDate, interval);

        return ChartBundleFactory.create(metadata, mainChart, indicators, comparisonCharts);

    }

}
