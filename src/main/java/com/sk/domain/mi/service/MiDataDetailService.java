package com.sk.domain.mi.service;

import com.sk.common.model.ValidationResult;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.ChartBundleFactory;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.chart.ChartBundleRes;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.dto.response.chart.IndicatorData;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final GoodsDataSourceValidator goodsDataSourceDetailValidator;

    private final UnitConversionService unitConversionService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;


//    /**
//     * 상품 데이터를 상세 조회한다.
//     *
//     * @param goodsDataNo 상품 데이터 기본
//     * @return
//     */
//    public GoodsDataDetailRes getGoodsData(Integer goodsDataNo) {
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        GoodsData goodsData = result.getOrThrow();
//        return null;
//    }
//
//    /**
//     * 데이터소스 목록을 조회한다.
//     *
//     * @param goodsDataNo 상품 데이터 번호
//     * @return
//     */
//    public List<GoodsDataSourceRes> getGoodsDataSources(Integer goodsDataNo) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        GoodsData goodsData = result.getOrThrow();
//
//        List<GoodsDataSourceDetail> dataSources = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(goodsData);
//
//        return GoodsDataSourceRes.ofList(dataSources);
//
//    }
//
//    /**
//     *
//     * @param goodsDataNo
//     * @param goodsDataSourceNo
//     * @return
//     */
//    public List<ChartsPersonalizeRes> getChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        result.validOrThrow();
//
//        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
//            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));
//
//        List<PersonalizedChartSetting> personalizedChartSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "");
//
//        return ChartsPersonalizeRes.ofList(personalizedChartSettings);
//    }
//
//    /**
//     *
//     * @param goodsDataNo
//     * @param goodsDataSourceNo
//     * @return
//     */
//    public List<UnitConversionRes> getGoodsDataSourcesUnitConversion(Integer goodsDataNo, Integer goodsDataSourceNo) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        GoodsData goodsData = result.getOrThrow();
//
//        return null;
//
//    }
//
//    /**
//     *
//     * @param goodsDataNo
//     * @param goodsDataSourceNo
//     * @param chartsPersonalizeReqs
//     * @return
//     */
//    @Transactional
//    public List<ChartsPersonalizeRes> putChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo, List<ChartsPersonalizeReq> chartsPersonalizeReqs) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        result.validOrThrow();
//
//        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
//            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));
//
//        List<PersonalizedChartSetting> personalizedChartSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "");
//        Map<ChartSettingConditionCd, PersonalizedChartSetting> personalSettingMap = personalizedChartSettings.stream().collect(Collectors.toMap(PersonalizedChartSetting::getChartSettingConditionCd, Function.identity()));
//
//        chartsPersonalizeReqs.forEach(req -> {
//
//            if (personalSettingMap.containsKey(req.chartSettingConditionCd())) {
//
//                PersonalizedChartSetting personalizedChartSetting = personalSettingMap.get(req.chartSettingConditionCd());
//
//                if (ObjectUtils.isEmpty(req.chartConditionValue())) {
//                    personalizedChartSetting.softDelete(); // 값이 null이면 soft delete
//                } else {
//                    personalizedChartSetting.update(req); // 값이 null이 아니면 update
//                }
//
//            } else {
//
//                PersonalizedChartSetting setting = PersonalizedChartSetting.of(req, goodsDataSourceDetail);// 새로운 key의 값이면 insert
//
//            }
//
//        });
//
//
//        return null; // todo
//    }

    /**
     * 상품 데이터소스 차트 묶음을 조회한다.
     *
     * @param goodsDataNo 상품 데이터 번호
     * @param goodsDataSourceNo 상품 데이터소스 번호
     * @return ChartBundleRes
     */
    public ChartBundleRes getGoodsDataSourceChart(Integer goodsDataNo, Integer goodsDataSourceNo) {

        // todo 개인화 추가...

        // 상품데이터 검증
        ValidationResult<GoodsData> goodsDataResult = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataResult.getOrThrow();

        // 컬럼 정보 Map 생성
        Map<String, String> dataColumnsMap = goodsData.getGoods().getProvider().getProviderDataColumns()
            .stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), ProviderDataColumn::getDataColumnExpn));

        // 상품 데이터소스 검증 및 '메인 차트' 메타 정보 조회
        ValidationResult<GoodsDataSourceDetail> goodsDataSourceResult = goodsDataSourceDetailValidator.ensureExists(goodsDataSourceNo);
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceResult.getOrThrow();

        // 상품 데이터소스 '메인 차트' 데이터 조회
        List<Map<String, Object>> goodsDataSourceCharts = getRows(goodsDataSourceDetail);

        // 데이터 변경
        ChartData chartData = ChartBundleFactory.convertToChartData(goodsDataSourceDetail.getIsCandleChartPossible(), goodsDataSourceCharts, goodsDataSourceDetail.getTableColumnListNm(), dataColumnsMap);

        // 보조지표를 위한 주요 메트릭 시계열 데이터 추출 ('close' 또는 'main')
        List<BigDecimal> majorMetricSeries = ChartBundleFactory.extractMajorMetricSeries(chartData);

        List<BigDecimal> ma5 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 5);
        List<BigDecimal> ma20 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 20);
        List<BigDecimal> ma60 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 60);
        List<BigDecimal> ma120 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 120);

        // 이동평균선(MA) 지표 데이터 생성
        IndicatorData maIndicator = ChartBundleFactory.createMovingAverageIndicator(chartData, ma5, ma20, ma60, ma120);

        List<BollingerBandResult> bollingerBandResults = unitConversionService.convertTimeSeriesToBollingerBands(majorMetricSeries, 20);

        // 볼린저 밴드(BB) 지표 데이터 생성
        IndicatorData bbIndicator = ChartBundleFactory.createBollingerBandsIndicator(chartData, bollingerBandResults);

        // 지표 데이터 목록 생성
        List<IndicatorData> indicators = List.of(maIndicator, bbIndicator);

        List<ChartData> compareDynamicChartRes = new ArrayList<>();

        // 상품 데이터소스 '비교군' 메타 정보 조회
        List<DataSourceGroupMapping> compareDataSources = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(goodsDataSourceDetail);

        compareDataSources.forEach(compareDataSource -> {
            GoodsDataSourceDetail compareGoodsDataSourceDetail = compareDataSource.getCompareGoodsDataSourceDetail();

            Map<String, String> compareDataColumnsMap = compareGoodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns()
                .stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), ProviderDataColumn::getDataColumnExpn));

            List<Map<String, Object>> cds = getRows(compareDataSource.getCompareGoodsDataSourceDetail());
            compareDynamicChartRes.add(ChartBundleFactory.convertToChartData(compareGoodsDataSourceDetail.getIsCandleChartPossible(), cds, compareGoodsDataSourceDetail.getTableColumnListNm(), compareDataColumnsMap));
        });


        return new ChartBundleRes(
            null,
            chartData,
            indicators,
            compareDynamicChartRes
        );
    }

    @Transactional(transactionManager = "myBatisTransactionManager")
    public List<Map<String, Object>> getRows(GoodsDataSourceDetail goodsDataSourceDetail) {
        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail));
    }

}
