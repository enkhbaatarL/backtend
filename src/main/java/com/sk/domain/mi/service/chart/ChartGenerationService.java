package com.sk.domain.mi.service.chart;

import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.charts.ChartRes;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 차트 데이터를 생성하는 서비스
 * 데이터소스로부터 데이터를 조회하고 차트 형태로 가공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartGenerationService {

    private static final int DATE_COLUMN_INDEX = 0;

    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    private final PersonalizationService personalizationService;
    private final ChartAggregationService chartAggregationService;
    private final ChartUnitConversionService chartUnitConversionService;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;

    /**
     * 특정 데이터소스와 시간 범위에 대한 차트 데이터를 생성한다.
     * 데이터소스로부터 원시 데이터를 조회하고, 인터벌에 따라 데이터를 집계한 후,
     * 단위 변환을 적용하여 최종 차트 데이터를 생성한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param startDate 조회 시작일 (YYYYMMDD 형식)
     * @param endDate 조회 종료일 (YYYYMMDD 형식)
     * @param interval 데이터 집계 시간 간격
     * @param unitConvert 단위 변환 정보
     * @param isVisible 차트 표시 여부
     * @return 처리된 차트 데이터
     */
    public ChartRes generateChartData(GoodsDataSourceDetail goodsDataSourceDetail, String startDate, String endDate, Interval interval, UnitConvert unitConvert, boolean isVisible) {

        Map<String, String> columnExpressions = getColumnExpressions(goodsDataSourceDetail);
        List<String> tartgetColumns = Arrays.stream(goodsDataSourceDetail.getTableColumnListNm().split(",")).toList();

        String dateColumnName = tartgetColumns.get(DATE_COLUMN_INDEX);
        boolean isCandleChartPossible = goodsDataSourceDetail.getIsCandleChartPossible();

        // Retrieve and process data
        List<Map<String, Object>> rawData = getRows(goodsDataSourceDetail, dateColumnName, startDate, endDate);
        if (rawData.isEmpty()) {
            // Return empty chart data by passing empty list to the factory
            return ChartBundleFactory.toChart(isCandleChartPossible, Collections.emptyList(), tartgetColumns, columnExpressions, isVisible);
        }

        List<Map<String, Object>> aggregatedData = chartAggregationService.aggregate(rawData, interval, tartgetColumns, goodsDataSourceDetail);

        List<Map<String, Object>> convertedData = chartUnitConversionService.convert(isCandleChartPossible, aggregatedData, tartgetColumns, unitConvert);

        // Convert to chart data format
        return ChartBundleFactory.toChart(isCandleChartPossible, convertedData, tartgetColumns, columnExpressions, isVisible);
    }

    /**
     * 데이터소스에서 컬럼 표현식을 추출한다.
     * 데이터소스 상세 정보에서 제공자 데이터 컬럼 정보를 추출하여 컬럼명과 표현식을 매핑한 맵을 생성한다.
     * 중복된 키가 있는 경우 첫 번째 값을 유지한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @return 컬럼명과 표현식을 매핑한 맵
     */
    private Map<String, String> getColumnExpressions(GoodsDataSourceDetail goodsDataSourceDetail) {
        if (goodsDataSourceDetail == null ||
            goodsDataSourceDetail.getGoodsData() == null ||
            goodsDataSourceDetail.getGoodsData().getGoods() == null ||
            goodsDataSourceDetail.getGoodsData().getGoods().getProvider() == null ||
            goodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns() == null) {
            return Collections.emptyMap();
        }

        return goodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns()
            .stream()
            .filter(c -> c != null && c.getId() != null)
            .collect(Collectors.toMap(
                c -> c.getId().getProviderDataColumnNm(),
                ProviderDataColumn::getDataColumnExpn,
                // In case of duplicate keys, keep the first one
                (first, second) -> first
            ));
    }

    /**
     * 메인 데이터소스에 대한 비교 차트를 생성한다.
     * 메인 데이터소스와 연결된 비교군 데이터소스를 찾아 각각에 대한 차트 데이터를 생성한다.
     * 개인화 설정이 있는 경우, 개인화된 비교군 데이터소스만 포함한다.
     *
     * @param mainDataSource 비교 대상 메인 데이터소스
     * @param startDate 조회 시작일 (YYYYMMDD 형식)
     * @param endDate 조회 종료일 (YYYYMMDD 형식)
     * @param interval 데이터 집계 시간 간격
     * @param personalizedMap 개인화 설정 맵
     * @return 비교군 데이터소스에 대한 차트 데이터 목록
     */
    public List<ChartRes> generateComparisonCharts(GoodsDataSourceDetail mainDataSource, String startDate, String endDate, Interval interval, Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        if (mainDataSource == null) {
            return Collections.emptyList();
        }

        // Find all comparison data sources for the main source
        List<DataSourceGroupMapping> comparisons = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(mainDataSource);

        if (comparisons.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> personalizedCompDataSources = personalizationService.getPersonalizedCompDataSources(personalizedMap);

        // Generate chart data for each comparison source, filtering by personalized settings if available
        return comparisons.stream()
            .filter(comparison -> comparison.getCompareGoodsDataSourceDetail() != null)
            .map(comparison -> generateChartData(comparison.getCompareGoodsDataSourceDetail(), startDate, endDate, interval, null, personalizedCompDataSources.contains(comparison.getId().getCompareGoodsDataSourceNo())))
            .toList();
    }

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일, 종료일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName 필터링할 날짜 컬럼명
     * @param startDate 데이터 범위 시작일 (포함, YYYYMMDD 형식)
     * @param endDate 데이터 범위 종료일 (포함, YYYYMMDD 형식)
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional(transactionManager = "myBatisTransactionManager")
    public List<Map<String, Object>> getRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String startDate, String endDate) {

        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        String whereClause = "%s between '%s' and '%s'".formatted(dateColumnName, startDate, endDate);
        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause));
    }

}
