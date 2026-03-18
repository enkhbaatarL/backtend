package com.sk.domain.mi.service.newsletter.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "NEWS_EVENT";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        if (pubHist == null) {
            return ResolvedBlockData.of(Map.of("items", List.of()));
        }

        List<NewsletterEvent> events = newsletterEventRepository.findByNewsletterPublishHistoryOrderByEventCreateTime(pubHist);
        Integer requestedNewsId = resolveNewsId(blockState);
        if (requestedNewsId != null) {
            return events.stream()
                .filter(event -> requestedNewsId.equals(event.getNewsletterEventId()))
                .findFirst()
                .map(this::toSingleEventContent)
                .map(ResolvedBlockData::of)
                .orElseGet(() -> ResolvedBlockData.error("NEWS_EVENT_NOT_FOUND"));
        }

        int maxItems = resolveMaxItems(block, blockState);
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

    private Integer resolveNewsId(IssueStateTree.BlockStateNode blockState) {
        if (blockState == null) {
            return null;
        }
        Object newsId = blockState.state().get("newsId");
        if (newsId instanceof Number number) {
            return number.intValue();
        }
        if (newsId != null) {
            try {
                return Integer.parseInt(String.valueOf(newsId));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int resolveMaxItems(TemplateTree.BlockNode block, IssueStateTree.BlockStateNode blockState) {
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
        return block.fields().stream()
            .filter(field -> field.fieldMeta() != null && !field.fieldMeta().isBlank())
            .findFirst()
            .map(field -> {
                try {
                    JsonNode meta = objectMapper.readTree(field.fieldMeta());
                    return meta.path("maxItems").asInt(20);
                } catch (Exception ignored) {
                    return 20;
                }
            })
            .orElse(20);
    }

    private Map<String, Object> toItem(NewsletterEvent event) {
        return Map.of(
            "newsId", String.valueOf(event.getNewsletterEventId()),
            "title", event.getEventTitle(),
            "content", toContentLines(event),
            "timestamp", event.getEventCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli(),
            "createdDate", event.getEventCreateTime().format(CREATED_DATE_FORMATTER),
            "markTime", event.getEventMarkTime(),
            "source", event.getEventProvider(),
            "isTurningPoint", "Y".equals(event.getTurningPointMarkYn()),
            "directionalityType", event.getDirectionalityType()
        );
    }

    private Map<String, Object> toSingleEventContent(NewsletterEvent event) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("newsId", String.valueOf(event.getNewsletterEventId()));
        content.put("title", event.getEventTitle());
        content.put("content", toContentLines(event));
        content.put("timestamp", event.getEventCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli());
        content.put("createdDate", event.getEventCreateTime().format(CREATED_DATE_FORMATTER));
        content.put("markTime", event.getEventMarkTime());
        content.put("source", event.getEventProvider());
        content.put("isTurningPoint", "Y".equals(event.getTurningPointMarkYn()));
        content.put("directionalityType", event.getDirectionalityType());
        return content;
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
