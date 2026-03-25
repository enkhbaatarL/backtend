package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
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
        NewsletterPublishHistory pubHist
    ) {
        if (pubHist == null) {
            Map<String, Object> emptyItem = emptyItem(Boolean.FALSE);
            return ResolvedBlockData.expanded(emptyItem, List.of(emptyItem));
        }

        List<NewsletterEvent> events = newsletterEventRepository.findByNewsletterPublishHistoryOrderByEventCreateTime(pubHist);
        try {
            List<Map<String, Object>> items = events.stream()
                .map(event -> toItem(event, null))
                .toList();
            if (items.isEmpty()) {
                Map<String, Object> emptyItem = emptyItem(Boolean.FALSE);
                return ResolvedBlockData.expanded(emptyItem, List.of(emptyItem));
            }
            return ResolvedBlockData.expanded(items.get(0), items);
        } catch (RuntimeException e) {
            log.warn("NEWS_ITEM 변환 실패 - templateBlockId: {}", block.id(), e);
            Map<String, Object> emptyItem = emptyItem(Boolean.FALSE);
            return ResolvedBlockData.expanded(emptyItem, List.of(emptyItem));
        }
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
        item.put("originalUrl", event.getOriginalUrl());
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
