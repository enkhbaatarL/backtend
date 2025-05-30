package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.charts.Chart;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 차트 데이터를 생성하는 서비스
 * 데이터소스로부터 데이터를 조회하고 차트 형태로 가공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartGenerationService {

    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    private final ChartAggregationService chartAggregationService;
    private final ChartUnitConversionService chartUnitConversionService;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;

    /**
     * 특정 데이터소스와 시간 범위에 대한 차트 데이터를 생성한다.
     * 데이터소스로부터 원시 데이터를 조회하고, 인터벌에 따라 데이터를 집계한 후,
     * 단위 변환을 적용하여 최종 차트 데이터를 생성한다.
     *
     * @param gds             데이터소스 상세 정보
     * @param determinedRange 조회 시작일, 조회 종료일 (YYYYMMDD 형식)
     * @param interval        데이터 집계 시간 간격
     * @param unitConvert     단위 변환 객체
     * @param isVisible       차트 표시 여부
     * @param isMainChart     메인 차트 여부
     * @return 처리된 차트 데이터
     */
    public Chart generateChartData(GoodsDataSourceDetail gds, Pair<String, String> determinedRange, Interval interval, UnitConvert unitConvert, boolean isVisible, boolean isMainChart) {

        // 컬럼 정보 추출
        Map<String, String> columnExpressions = getColumnExpressions(gds);
        List<String> targetColumns = Arrays.stream(gds.getTableColumnListNm().split(",")).map(String::trim).toList();

        String dateColumnName = targetColumns.get(RenderAs.DATE.getIndex()).trim();
        String volumeColumnNm = Optional.ofNullable(gds.getTradeVolumeColumnNm()).map(String::trim).orElse(null);
        boolean isCandleChartPossible = gds.getIsCandleChartPossible();

        // 원시 데이터 : [조회]
        List<Map<String, Object>> rawData = getRows(gds, dateColumnName, determinedRange, isMainChart);

        // 원시 데이터 : [정렬 및 날짜 변환]
        List<Map<String, Object>> sortedData = convertAndOrderByDates(rawData, dateColumnName);

        // 원시 데이터 : [집계]
        List<Map<String, Object>> aggregatedData = chartAggregationService.aggregate(sortedData, interval, targetColumns, gds);

        // 원시 데이터 : [단위 변환]
        List<Map<String, Object>> convertedData = chartUnitConversionService.convert(isCandleChartPossible, aggregatedData, targetColumns, volumeColumnNm, unitConvert, isMainChart);

        return ChartBundleFactory.combineCharts(gds, isCandleChartPossible, convertedData, targetColumns, columnExpressions, isVisible);
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
     * @param mainDataSource   비교 대상 메인 데이터소스
     * @param determinedRange  조회 시작일, 조회 종료일 (YYYYMMDD 형식)
     * @param interval         데이터 집계 시간 간격
     * @param finalizedCompIds 보여줄 비교군 데이터 소스 목록
     * @return 비교군 데이터소스에 대한 차트 데이터 목록
     */
    public List<Chart> generateComparisonCharts(GoodsDataSourceDetail mainDataSource, Pair<String, String> determinedRange, Interval interval, List<Integer> finalizedCompIds) {

        // 유효성 검증
        if (mainDataSource == null) {
            return Collections.emptyList();
        }

        // 비교 데이터소스 조회
        List<DataSourceGroupMapping> comparisons = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(mainDataSource);

        if (comparisons.isEmpty()) {
            return Collections.emptyList();
        }


        // 각 비교 데이터소스에 대한 차트 데이터 생성
        return comparisons.stream()
            .filter(comparison -> comparison.getCompareGoodsDataSourceDetail() != null)
            .map(comparison -> {
                boolean isVisible = finalizedCompIds.contains(comparison.getId().getCompareGoodsDataSourceNo());
                return generateChartData(
                    comparison.getCompareGoodsDataSourceDetail(),
                    determinedRange,
                    interval,
                    ChartUnitConversionService.DEFAULT_IDENTITY,
                    isVisible, // '비교군' 차트 화면 노출 여부
                    Boolean.FALSE // 메인 차트 여부
                );
            })
            .toList();
    }

    @Transactional(transactionManager = "myBatisTransactionManager")
    public LocalDate getLatestDate(GoodsDataSourceDetail gds) {

        if (gds == null) return null;

        List<String> targetColumns = Arrays.stream(gds.getTableColumnListNm().split(",")).map(String::trim).toList();
        String dateColumnName = targetColumns.get(RenderAs.DATE.getIndex()).trim();

        String selectClause = "max(%s) as latestDate".formatted(dateColumnName);
        List<Map<String, Object>> data = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.ofMaxQuery(gds, selectClause));

        return DateUtil.parseDate(data.get(0).get("latestDate"));
    }

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일, 종료일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param gds             데이터소스 상세 정보
     * @param dateColumnName  필터링할 날짜 컬럼명
     * @param determinedRange 데이터 범위 시작일, 데이터 범위 종료일 (포함, YYYYMMDD 형식)
     * @param isMainChart     메인 차트 여부
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional(transactionManager = "myBatisTransactionManager")
    public List<Map<String, Object>> getRows(GoodsDataSourceDetail gds, String dateColumnName, Pair<String, String> determinedRange, boolean isMainChart) {

        if (gds == null || dateColumnName == null || determinedRange == null) {
            return Collections.emptyList();
        }

        // 2025-05-23 fix: 메인차트인 경우 ma120을 표현하기 위해 180일의 LOOK_BACK Period를 설정
        String startDate = isMainChart ? DateUtil.minusDays(determinedRange.getLeft(), 180) : determinedRange.getLeft();

        String whereClause = "%s between '%s' and '%s'".formatted(dateColumnName, startDate, determinedRange.getRight());

        // select 절에 거래량 컬럼 추가
        String selectClause = Optional.ofNullable(gds.getTradeVolumeColumnNm())
            .filter(s -> isMainChart)
            .map(s -> gds.getTableColumnListNm() + "," + s)
            .orElse(gds.getTableColumnListNm());

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(gds, selectClause, whereClause, dateColumnName));
    }

    /**
     * 원시 데이터의 날짜 값을 LocalDate 타입으로 파싱하고, 정렬한다.
     * 실제 쿼리에서 orderBy를 통해 정렬이 수행되지만 서비스단에서 방어로직 차원에서 한번 더 진행한다.
     */
    private List<Map<String, Object>> convertAndOrderByDates(List<Map<String, Object>> rawData, String dateColumnKey) {
        return rawData.stream()
            .map(row -> {
                Map<String, Object> copy = new HashMap<>(row);
                Object raw = row.get(dateColumnKey);
                LocalDate parsed = DateUtil.parseDate(raw);
                copy.put(dateColumnKey, parsed);
                return copy;
            }).sorted(Comparator.comparing(
                (Map<String, Object> r) -> (LocalDate) r.get(dateColumnKey),
                Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }
}
