package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterEvent;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.repository.master.tmd.NewsletterEventRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("NEWS_EVENT")
@RequiredArgsConstructor
public class NewsEventBlockDataStrategy implements BlockDataStrategy {

    private static final DateTimeFormatter CREATED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final NewsletterEventRepository newsletterEventRepository;

    @Override
    public String getType() {
        return "NEWS_EVENT";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        BlockExecutionPlan executionPlan,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        Map<String, Object> content;
        if (pubHist == null) {
            content = emptyItem(Boolean.FALSE);
        } else {
            List<NewsletterEvent> events = newsletterEventRepository.findByNewsletterPublishHistoryOrderByEventCreateTime(pubHist);
            int index = resolveItemIndex(blockState);
            try {
                content = index >= events.size()
                    ? emptyItem(resolveShowInChart(blockState, Boolean.FALSE))
                    : toItem(events.get(index), resolveShowInChart(blockState, null));
            } catch (RuntimeException e) {
                log.warn("NEWS_ITEM 변환 실패 - issueBlockStateId: {}, index: {}", blockState != null ? blockState.issueBlockStateId() : null, index, e);
                content = emptyItem(resolveShowInChart(blockState, Boolean.FALSE));
            }
        }
        content.putAll(resolveContentInput(blockState));
        return ResolvedBlockData.of(content);
    }

    private int resolveItemIndex(IssueStateTree.BlockStateNode blockState) {
        if (blockState == null || blockState.order() == null) {
            return 0;
        }
        return Math.max(blockState.order() - 1, 0);
    }

    private Boolean resolveShowInChart(IssueStateTree.BlockStateNode blockState, Boolean fallback) {
        if (blockState == null) {
            return fallback;
        }
        if (blockState.plotOnChart() != null) {
            return blockState.plotOnChart();
        }
        Object raw = blockState.state().get("plotOnChart");
        if (raw instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return fallback;
    }

    private Map<String, Object> toItem(NewsletterEvent event, Boolean showInChartOverride) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("newsId", event.getNewsletterEventId() != null ? String.valueOf(event.getNewsletterEventId()) : null);
        item.put("title", event.getEventTitle());
        item.put("content", toContentLines(event));
        item.put("timestamp", event.getEventCreateTime() != null ? event.getEventCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli() : null);
        item.put("createdDate", event.getEventCreateTime() != null ? event.getEventCreateTime().format(CREATED_DATE_FORMATTER) : null);
        item.put("createTime", event.getEventMarkTime());
        item.put("source", event.getEventProvider());
        item.put("originalUrl", null);
        item.put("showInChart", showInChartOverride != null ? showInChartOverride : "Y".equals(event.getTurningPointMarkYn()));
        item.put("trend", event.getDirectionalityType());
        return item;
    }

    private Map<String, Object> emptyItem(Boolean showInChart) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("newsId", null);
        item.put("title", null);
        item.put("content", List.of());
        item.put("timestamp", null);
        item.put("createdDate", null);
        item.put("createTime", null);
        item.put("source", null);
        item.put("originalUrl", null);
        item.put("showInChart", showInChart);
        item.put("trend", null);
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveContentInput(IssueStateTree.BlockStateNode blockState) {
        if (blockState == null || blockState.state().isEmpty()) {
            return Map.of();
        }
        Object contentInput = blockState.state().get("contentInput");
        if (contentInput instanceof Map<?, ?> inputMap) {
            return new LinkedHashMap<>((Map<String, Object>) inputMap);
        }
        return Map.of();
    }

    private List<String> toContentLines(NewsletterEvent event) {
        if (event.getEventContent() == null || event.getEventContent().isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : event.getEventContent().split("\n")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }
}
