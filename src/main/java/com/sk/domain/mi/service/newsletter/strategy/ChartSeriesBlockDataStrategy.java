package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.common.utils.CombineUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.NewsletterChartSupport;
import com.sk.domain.mi.service.newsletter.NewsletterChartSupport.CollectionSeriesData;
import com.sk.domain.shared.entity.master.tif.NewsletterChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateDedicatedChart;
import com.sk.domain.shared.repository.master.tif.NewsletterChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterCollectionChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateCollectionChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateDedicatedChartRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("CHART_SERIES")
@RequiredArgsConstructor
public class ChartSeriesBlockDataStrategy implements BlockDataStrategy {

    private final NewsletterChartRepository newsletterChartRepository;
    private final NewsletterTemplateDedicatedChartRepository dedicatedChartRepository;
    private final NewsletterTemplateCollectionChartRepository collectionChartRepository;
    private final NewsletterCollectionChartRepository newsletterCollectionChartRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final NewsletterChartSupport chartSupport;

    @Override
    public String getType() {
        return "CHART_SERIES";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        try {
            String ref = block.dataSourceRef();
            if (ref == null || ref.isBlank()) {
                List<NewsletterChart> charts = newsletterChartRepository.findByNewsletterPublishHistoryAndCode(pubHist, block.blockCode());
                return ResolvedBlockData.of(chartSupport.toMainChartContent(charts, pubHist.getPublishDateTime()));
            }

            if (ref.startsWith("dedi:")) {
                String dataCode = ref.substring("dedi:".length());
                List<NewsletterTemplateDedicatedChart> configs = dedicatedChartRepository.findByDedicatedChartTypeAndIsDeleted(dataCode, false);
                Map<String, List<NewsletterChart>> chartsByCode = new HashMap<>();
                configs.forEach(config -> chartsByCode.put(config.getCode(), newsletterChartRepository.findByNewsletterPublishHistoryAndCode(pubHist, config.getCode())));
                return ResolvedBlockData.of(chartSupport.toDedicatedChartContent(configs, chartsByCode, pubHist.getPublishDateTime()));
            }

            if (ref.startsWith("cltn:")) {
                String dataCode = ref.substring("cltn:".length());
                NewsletterCollectionChart collectionChart = newsletterCollectionChartRepository
                    .findTopByNewsletterPublishHistoryAndCollectionChartTypeAndIsDeletedFalse(pubHist, dataCode)
                    .orElse(null);
                if (collectionChart == null) {
                    return ResolvedBlockData.error("COLLECTION_CHART_NOT_FOUND");
                }
                List<NewsletterTemplateCollectionChart> configs = collectionChartRepository.findByCollectionChartTypeAndIsDeleted(dataCode, false);
                List<CollectionSeriesData> seriesDataList = buildCollectionSeries(configs, collectionChart);
                return ResolvedBlockData.of(chartSupport.toCollectionChartContent(configs, collectionChart, seriesDataList, pubHist.getPublishDateTime()));
            }

            List<NewsletterChart> charts = newsletterChartRepository.findByNewsletterPublishHistoryAndCode(pubHist, ref);
            return ResolvedBlockData.of(chartSupport.toMainChartContent(charts, pubHist.getPublishDateTime()));
        } catch (Exception e) {
            log.error("차트 블록 resolve 실패 - blockCode: {}", block.blockCode(), e);
            return ResolvedBlockData.error("CHART_SERIES_RESOLVE_FAILED");
        }
    }

    private List<CollectionSeriesData> buildCollectionSeries(
        List<NewsletterTemplateCollectionChart> configs,
        NewsletterCollectionChart collectionChart
    ) {
        List<CollectionSeriesData> seriesDataList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (NewsletterTemplateCollectionChart config : configs) {
            try {
                String col1 = config.getTableColumnName().split(",")[0];
                String col2 = config.getTableColumnName().split(",")[1];
                DynamicDataReq req = DynamicDataReq.builder()
                    .targetDataSource(config.getTargetDatabaseName())
                    .tableName(config.getTargetTableName())
                    .columns(config.getTableColumnName())
                    .whereClause(config.getQueryConditionContent())
                    .additionalWhereClause(CombineUtil.createBetweenClause(
                        col1,
                        collectionChart.getStartDate().format(formatter),
                        collectionChart.getEndDate().format(formatter)
                    ))
                    .build();
                List<Map<String, Object>> data = mybatisDynamicQueryService.getDynamicData(req);
                Map<Long, BigDecimal> valuesByTimestamp = new HashMap<>();
                for (Map<String, Object> row : data) {
                    LocalDateTime dateTime = chartSupport.convertToLocalDateTime(row.get(col1));
                    Object value = row.get(col2);
                    if (dateTime != null && value instanceof BigDecimal bigDecimal) {
                        valuesByTimestamp.put(Timestamp.valueOf(dateTime).getTime(), bigDecimal);
                    }
                }
                List<Long> axis = chartSupport.buildTimeAxisTimestamps(
                    collectionChart.getStartDate(),
                    collectionChart.getEndDate(),
                    config.getTimeAxisIntervalValue(),
                    config.getTimeAxisIntervalUnit()
                );
                if (axis.isEmpty()) {
                    axis = valuesByTimestamp.keySet().stream().sorted().toList();
                }
                seriesDataList.add(new CollectionSeriesData(config, axis, valuesByTimestamp));
            } catch (Exception e) {
                log.error("수집 차트 시리즈 구성 실패 - color: {}", config.getColor(), e);
            }
        }
        return seriesDataList;
    }
}
