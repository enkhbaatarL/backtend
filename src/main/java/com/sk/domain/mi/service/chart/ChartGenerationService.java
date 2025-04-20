package com.sk.domain.mi.service.chart;

import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.enums.Interval;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for generating chart data
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartGenerationService {

    private static final int DATE_COLUMN_INDEX = 0;

    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    private final ChartAggregationService chartAggregationService;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;

    /**
     * Generates chart data for a specific data source and time range
     *
     * @param goodsDataSourceDetail The data source details
     * @param startDate The start date for the data range
     * @param endDate The end date for the data range
     * @param interval The time interval for data aggregation
     * @return ChartData containing the processed chart data
     */
    public ChartData generateChartData(GoodsDataSourceDetail goodsDataSourceDetail, String startDate, String endDate, Interval interval) {

        Map<String, String> columnExpressions = getColumnExpressions(goodsDataSourceDetail);
        String[] columns = goodsDataSourceDetail.getTableColumnListNm().split(",");

        String dateColumnName = columns[DATE_COLUMN_INDEX];
        boolean isCandleChartPossible = goodsDataSourceDetail.getIsCandleChartPossible();

        // Retrieve and process data
        List<Map<String, Object>> rawData = getRows(goodsDataSourceDetail, dateColumnName, startDate, endDate);
        if (rawData.isEmpty()) {
            // Return empty chart data by passing empty list to the factory
            return ChartBundleFactory.toChartData(isCandleChartPossible, Collections.emptyList(), goodsDataSourceDetail.getTableColumnListNm(), columnExpressions);
        }

        List<Map<String, Object>> aggregatedData = chartAggregationService.aggregate(rawData, interval, goodsDataSourceDetail);

        // Convert to chart data format
        return ChartBundleFactory.toChartData(isCandleChartPossible, aggregatedData, goodsDataSourceDetail.getTableColumnListNm(), columnExpressions);
    }

    /**
     * Extracts column expressions from the data source
     *
     * @param goodsDataSourceDetail The data source details
     * @return Map of column names to their expressions
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
     * Generates comparison charts for the main data source
     *
     * @param mainDataSource The main data source to find comparisons for
     * @param startDate The start date for the data range
     * @param endDate The end date for the data range
     * @param interval The time interval for data aggregation
     * @return List of chart data for comparison sources
     */
    public List<ChartData> generateComparisonCharts(GoodsDataSourceDetail mainDataSource, String startDate, String endDate, Interval interval) {

        if (mainDataSource == null) {
            return Collections.emptyList();
        }

        // Find all comparison data sources for the main source
        List<DataSourceGroupMapping> comparisons = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(mainDataSource);

        if (comparisons.isEmpty()) {
            return Collections.emptyList();
        }

        // Generate chart data for each comparison source
        return comparisons.stream()
            .filter(comparison -> comparison.getCompareGoodsDataSourceDetail() != null)
            .map(comparison -> generateChartData(comparison.getCompareGoodsDataSourceDetail(), startDate, endDate, interval))
            .toList();
    }

    /**
     * Retrieves data rows from the database based on date range
     *
     * @param goodsDataSourceDetail The data source details
     * @param dateColumnName The name of the date column for filtering
     * @param startDate The start date for the data range (inclusive)
     * @param endDate The end date for the data range (inclusive)
     * @return List of data rows matching the criteria
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
