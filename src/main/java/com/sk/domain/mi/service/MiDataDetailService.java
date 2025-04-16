package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.ChartDataConverter;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.ConversionResult;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.chart.ChartsPersonalizeRes;
import com.sk.domain.mi.dto.response.GoodsDataDetailRes;
import com.sk.domain.mi.dto.response.chart.GoodsDataSourceRes;
import com.sk.domain.mi.dto.response.chart.UnitConversionRes;
import com.sk.domain.mi.dto.response.chart.ChartBundleRes;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.enums.ChartSettingConditionCd;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final UnitConversionService unitConversionService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;


    /**
     * 상품 데이터를 상세 조회한다.
     *
     * @param goodsDataNo 상품 데이터 기본
     * @return
     */
    public GoodsDataDetailRes getGoodsData(Integer goodsDataNo) {
        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = result.getOrThrow();
        return null;
    }

    /**
     * 데이터소스 목록을 조회한다.
     *
     * @param goodsDataNo 상품 데이터 번호
     * @return
     */
    public List<GoodsDataSourceRes> getGoodsDataSources(Integer goodsDataNo) {

        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = result.getOrThrow();

        List<GoodsDataSourceDetail> dataSources = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(goodsData);

        return GoodsDataSourceRes.ofList(dataSources);

    }

    /**
     *
     * @param goodsDataNo
     * @param goodsDataSourceNo
     * @return
     */
    public List<ChartsPersonalizeRes> getChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo) {

        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
        result.validOrThrow();

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));

        List<PersonalizedChartSetting> personalizedChartSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "");

        return ChartsPersonalizeRes.ofList(personalizedChartSettings);
    }

    /**
     *
     * @param goodsDataNo
     * @param goodsDataSourceNo
     * @return
     */
    public List<UnitConversionRes> getGoodsDataSourcesUnitConversion(Integer goodsDataNo, Integer goodsDataSourceNo) {

        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = result.getOrThrow();

        return null;

    }

    /**
     *
     * @param goodsDataNo
     * @param goodsDataSourceNo
     * @param chartsPersonalizeReqs
     * @return
     */
    @Transactional
    public List<ChartsPersonalizeRes> putChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo, List<ChartsPersonalizeReq> chartsPersonalizeReqs) {

        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
        result.validOrThrow();

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));

        List<PersonalizedChartSetting> personalizedChartSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "");
        Map<ChartSettingConditionCd, PersonalizedChartSetting> personalSettingMap = personalizedChartSettings.stream().collect(Collectors.toMap(PersonalizedChartSetting::getChartSettingConditionCd, Function.identity()));

        chartsPersonalizeReqs.forEach(req -> {

            if (personalSettingMap.containsKey(req.chartSettingConditionCd())) {

                PersonalizedChartSetting personalizedChartSetting = personalSettingMap.get(req.chartSettingConditionCd());

                if (ObjectUtils.isEmpty(req.chartConditionValue())) {
                    personalizedChartSetting.softDelete(); // 값이 null이면 soft delete
                } else {
                    personalizedChartSetting.update(req); // 값이 null이 아니면 update
                }

            } else {

                PersonalizedChartSetting setting = PersonalizedChartSetting.of(req, goodsDataSourceDetail);// 새로운 key의 값이면 insert

            }

        });


        return null; // todo
    }

    /**
     * 상품 데이터소스 차트를 조회한다.
     *
     * @param goodsDataNo 상품 데이터 번호
     * @param goodsDataSourceNo 상품 데이터소스 번호
     * @return
     */
    public ChartBundleRes getGoodsDataSourceChart(Integer goodsDataNo, Integer goodsDataSourceNo) {

        // todo 개인화 추가...

        ValidationResult<GoodsData> goodsDataValid = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataValid.getOrThrow();

        // 프로바이더 공통 컬럼 정보
        List<ProviderDataColumn> dataColumns = goodsData.getGoods().getProvider().getProviderDataColumns();
        // Map 생성(key 컬럼명, value 범례)
        Map<String, String> dataColumnsMap = dataColumns.stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), ProviderDataColumn::getDataColumnExpn));

        // 상품 데이터소스 '메인' 메타 정보 조회
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));

        // 상품 데이터소스 '메인' 차트 데이터 조회
        List<Map<String, Object>> goodsDataSourceCharts = getRows(goodsDataSourceDetail);

        // 데이터 변경
        ChartData chartData = ChartDataConverter.convertToChartData(goodsDataSourceCharts, goodsDataSourceDetail.getTableColumnListNm(), dataColumnsMap);

        List<BigDecimal> majorFactor = ChartDataConverter.extractMainSeries(chartData);

        List<BigDecimal> ma5 = unitConversionService.convertTimeSeriesForMA(majorFactor, 5);
        List<BigDecimal> ma20 = unitConversionService.convertTimeSeriesForMA(majorFactor, 20);
        List<BigDecimal> ma60 = unitConversionService.convertTimeSeriesForMA(majorFactor, 60);
        List<BigDecimal> ma120 = unitConversionService.convertTimeSeriesForMA(majorFactor, 120);

        List<? extends ConversionResult> bb = unitConversionService.convertTimeSeriesForBB(majorFactor, 20);

        List<ChartData> compareDynamicChartRes = new ArrayList<>();

        // 상품 데이터소스 '비교군' 메타 정보 조회
        List<DataSourceGroupMapping> compareDataSources = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(goodsDataSourceDetail);

        compareDataSources.forEach(compareDataSource -> {
            GoodsDataSourceDetail compareGoodsDataSourceDetail = compareDataSource.getCompareGoodsDataSourceDetail();

            List<ProviderDataColumn> compareDataColumns = compareGoodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns();
            Map<String, String> compareDataColumnsMap = compareDataColumns.stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), c -> c.getDataColumnExpn()));

            List<Map<String, Object>> cds = getRows(compareDataSource.getCompareGoodsDataSourceDetail());
            compareDynamicChartRes.add(ChartDataConverter.convertToChartData(cds, compareGoodsDataSourceDetail.getTableColumnListNm(), compareDataColumnsMap));
        });

        return null;
    }

    @Transactional(transactionManager = "myBatisTransactionManager")
    public List<Map<String, Object>> getRows(GoodsDataSourceDetail goodsDataSourceDetail) {
        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail));
    }

}
