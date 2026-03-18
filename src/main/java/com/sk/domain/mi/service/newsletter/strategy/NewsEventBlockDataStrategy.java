package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.NewsletterChartSupport;
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
import org.springframework.stereotype.Component;

@Component("NEWS_EVENT")
@RequiredArgsConstructor
public class NewsEventBlockDataStrategy implements BlockDataStrategy {

    private static final DateTimeFormatter CREATED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final NewsletterEventRepository newsletterEventRepository;
    private final NewsletterChartSupport chartSupport;

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
        if (pubHist == null) {
            return ResolvedBlockData.of(Map.of("aggregationTime", null, "items", List.of()));
        }

        List<NewsletterEvent> events = newsletterEventRepository.findByNewsletterPublishHistoryOrderByEventCreateTime(pubHist);
        int maxItems = resolveMaxItems(executionPlan, blockState);
        List<Map<String, Object>> items = events.stream()
            .limit(maxItems)
            .map(this::toItem)
            .toList();

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("aggregationTime", chartSupport.calculateMarketTimes(pubHist.getPublishDateTime())[2] != null
            ? "뉴욕시간 전일 20:00부터 당일 17:00까지" : null);
        content.put("items", items);
        return ResolvedBlockData.of(content);
    }

    private int resolveMaxItems(BlockExecutionPlan executionPlan, IssueStateTree.BlockStateNode blockState) {
        if (blockState != null) {
            Object maxItems = blockState.queryParams().get("maxItems");
            if (maxItems instanceof Number number) {
                return number.intValue();
            }
            if (maxItems != null) {
                try {
                    return Integer.parseInt(String.valueOf(maxItems));
                } catch (NumberFormatException ignored) {
                    return 20;
                }
            }
        }
        if (executionPlan.isCanonical() && executionPlan.spec() != null && executionPlan.spec().query() != null) {
            Object maxItems = executionPlan.spec().query().get("maxItems");
            if (maxItems instanceof Number number) {
                return number.intValue();
            }
            if (maxItems != null) {
                try {
                    return Integer.parseInt(String.valueOf(maxItems));
                } catch (NumberFormatException ignored) {
                    return 20;
                }
            }
        }
        return 20;
    }

    private Map<String, Object> toItem(NewsletterEvent event) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("newsId", event.getNewsletterEventId() != null ? String.valueOf(event.getNewsletterEventId()) : null);
        item.put("title", event.getEventTitle());
        item.put("content", toContentLines(event));
        item.put("timestamp", event.getEventCreateTime() != null ? event.getEventCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli() : null);
        item.put("createdDate", event.getEventCreateTime() != null ? event.getEventCreateTime().format(CREATED_DATE_FORMATTER) : null);
        item.put("markTime", event.getEventMarkTime());
        item.put("source", event.getEventProvider());
        item.put("isTurningPoint", "Y".equals(event.getTurningPointMarkYn()));
        item.put("directionalityType", event.getDirectionalityType());
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
