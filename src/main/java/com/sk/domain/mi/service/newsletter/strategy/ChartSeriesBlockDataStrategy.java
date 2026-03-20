package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.common.utils.CombineUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.NewsletterChartSupport;
import com.sk.domain.mi.service.newsletter.NewsletterChartSupport.CollectionSeriesData;
import com.sk.domain.mi.service.newsletter.StaticBlockDefaultValueResolver;
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
    private final StaticBlockDefaultValueResolver defaultValueResolver;

    @Override
    public String getType() {
        return "CHART_SERIES";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        BlockExecutionPlan executionPlan,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        try {
            Map<String, Object> content = executionPlan.isCanonical()
                ? resolveCanonical(block, blockState, executionPlan, pubHist)
                : resolveLegacy(block, pubHist);
            return ResolvedBlockData.of(content);
        } catch (Exception e) {
            log.error("차트 블록 resolve 실패 - blockCode: {}", block.blockCode(), e);
            return ResolvedBlockData.error("CHART_SERIES_RESOLVE_FAILED");
        }
    }

    private Map<String, Object> resolveCanonical(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        BlockExecutionPlan executionPlan,
        NewsletterPublishHistory pubHist
    ) {
        String locatorKind = executionPlan.spec().locator().kind();
        String refCode = executionPlan.spec().locator().refCode();
        if ("MAIN_CHART".equals(locatorKind) && (refCode == null || refCode.isBlank())) {
            refCode = block.blockCode();
        }
        Map<String, Object> content = new HashMap<>(defaultValueResolver.resolve(block, pubHist));
        content.putAll(resolveChartBlockState(block, blockState));
        content.putAll(resolveChartPayload(block, pubHist, locatorKind, refCode));
        return content;
    }

    private Map<String, Object> resolveLegacy(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist) {
        String ref = block.dataSourceRef();
        if (ref == null || ref.isBlank()) {
            return resolveChartPayload(block, pubHist, "MAIN_CHART", block.blockCode());
        }
        if (ref.startsWith("dedi:")) {
            return resolveChartPayload(block, pubHist, "DEDICATED_CHART", ref.substring("dedi:".length()));
        }
        if (ref.startsWith("cltn:")) {
            return resolveChartPayload(block, pubHist, "COLLECTION_CHART", ref.substring("cltn:".length()));
        }
        return resolveChartPayload(block, pubHist, "MAIN_CHART", ref);
    }

    private Map<String, Object> resolveChartPayload(
        TemplateTree.BlockNode block,
        NewsletterPublishHistory pubHist,
        String locatorKind,
        String refCode
    ) {
        if ("DEDICATED_CHART".equals(locatorKind)) {
            List<NewsletterTemplateDedicatedChart> configs = dedicatedChartRepository.findByDedicatedChartTypeAndIsDeleted(refCode, false);
            Map<String, List<NewsletterChart>> chartsByCode = new HashMap<>();
            configs.forEach(config -> chartsByCode.put(config.getCode(), newsletterChartRepository.findByNewsletterPublishHistoryAndCode(pubHist, config.getCode())));
            // chartSupport가 불변 Map을 반환할 수 있으므로 호출자가 수정할 수 있도록 가변 Map으로 복사한다.
            return new HashMap<>(chartSupport.toDedicatedChartContent(configs, chartsByCode, pubHist.getPublishDateTime()));
        }

        if ("COLLECTION_CHART".equals(locatorKind)) {
            NewsletterCollectionChart collectionChart = newsletterCollectionChartRepository
                .findTopByNewsletterPublishHistoryAndCollectionChartTypeAndIsDeletedFalse(pubHist, refCode)
                .orElse(null);
            if (collectionChart == null) {
                return Map.of("timestamps", List.of(), "series", List.of());
            }
            List<NewsletterTemplateCollectionChart> configs = collectionChartRepository.findByCollectionChartTypeAndIsDeleted(refCode, false);
            List<CollectionSeriesData> seriesDataList = buildCollectionSeries(configs, collectionChart);
            // chartSupport가 불변 Map을 반환할 수 있으므로 호출자가 수정할 수 있도록 가변 Map으로 복사한다.
            return new HashMap<>(chartSupport.toCollectionChartContent(configs, collectionChart, seriesDataList, pubHist.getPublishDateTime()));
        }

        List<NewsletterChart> charts = newsletterChartRepository.findByNewsletterPublishHistoryAndCode(pubHist, refCode);
        Map<String, Object> content = new HashMap<>(chartSupport.toMainChartContent(charts, pubHist.getPublishDateTime()));
        if ("NEWS_FLOW_CHART".equals(block.blockCode()) && !content.containsKey("title")) {
            content.put("title", block.blockName());
        }
        return content;
    }

    private Map<String, Object> resolveChartBlockState(TemplateTree.BlockNode block, IssueStateTree.BlockStateNode blockState) {
        if (blockState == null || blockState.state().isEmpty()) {
            return Map.of();
        }
        Map<String, Object> content = new HashMap<>();
        block.fields().forEach(field -> {
            Object value = blockState.state().get(field.fieldKey());
            if (value != null) {
                content.put(field.fieldKey(), value);
            }
        });
        return content;
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
