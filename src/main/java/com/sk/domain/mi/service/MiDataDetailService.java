package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.ChartDataConverter;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.ConversionResult;
import com.sk.domain.mi.dto.response.GoodsDataDetailRes;
import com.sk.domain.mi.dto.response.GoodsDataSourceRes;
import com.sk.domain.mi.dto.response.chart2.ChartBundleRes;
import com.sk.domain.mi.dto.response.chart2.ChartData;
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
     * 상품 데이터소스 차트를 조회한다.
     *
     * @param goodsDataNo 상품 데이터 번호
     * @param dataSourceNo 상품 데이터소스 번호
     * @return
     */
    public ChartBundleRes getGoodsDataSourceChart(Integer goodsDataNo, Integer dataSourceNo) {

        ValidationResult<GoodsData> goodsDataValid = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataValid.getOrThrow();

        // 프로바이더 컬럼 정보
        List<ProviderDataColumn> dataColumns = goodsData.getGoods().getProvider().getProviderDataColumns();

        // Map 생성(key 컬럼명, value 범례)
        Map<String, String> dataColumnsMap = dataColumns.stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), ProviderDataColumn::getDataColumnExpn));

        // 상품 데이터소스 '메인' 메타 정보 조회
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(dataSourceNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(dataSourceNo)));

        // 상품 데이터소스 '메인' 차트 데이터 조회
        List<Map<String, Object>> goodsDataSourceCharts = getGoodsDataSourceCharts(goodsDataSourceDetail);

        // 데이터 변경
        ChartData chartData = ChartDataConverter.convertToChartData(goodsDataSourceCharts, goodsDataSourceDetail.getTableColumnListNm(), dataColumnsMap);

        List<BigDecimal> values = ChartDataConverter.extractMainSeries(chartData);

        // todo 보조지표(MA5~MA120) 조회
        List<BigDecimal> ma5 = unitConversionService.convertTimeSeriesForMA(values, 5);
        List<BigDecimal> ma20 = unitConversionService.convertTimeSeriesForMA(values, 20);
        List<BigDecimal> ma60 = unitConversionService.convertTimeSeriesForMA(values, 60);
        List<BigDecimal> ma120 = unitConversionService.convertTimeSeriesForMA(values, 120);

        // todo 보조지표(BB) 조회
        List<? extends ConversionResult> bb = unitConversionService.convertTimeSeriesForBB(values, 20);

        List<ChartData> compareDynamicChartRes = new ArrayList<>();

        // 상품 데이터소스 '비교군' 조회
        List<DataSourceGroupMapping> compareDataSources = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(goodsDataSourceDetail);
        compareDataSources.forEach(compareDataSource -> {
            GoodsDataSourceDetail compareGoodsDataSourceDetail = compareDataSource.getCompareGoodsDataSourceDetail();

            List<ProviderDataColumn> compareDataColumns = compareGoodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns();
            Map<String, String> compareDataColumnsMap = compareDataColumns.stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), c -> c.getDataColumnExpn()));

            List<Map<String, Object>> cds = getGoodsDataSourceCharts(compareDataSource.getCompareGoodsDataSourceDetail());
            compareDynamicChartRes.add(ChartDataConverter.convertToChartData(cds, compareGoodsDataSourceDetail.getTableColumnListNm(), compareDataColumnsMap));
        });

        return null;
    }

    @Transactional(transactionManager = "dynamicTransactionManager")
    public List<Map<String, Object>> getGoodsDataSourceCharts(GoodsDataSourceDetail goodsDataSourceDetail) {
        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail));
    }

}
