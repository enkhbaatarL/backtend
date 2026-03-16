package com.sk.domain.mi.service.newsletter.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.NewsletterChartSupport;
import com.sk.domain.shared.entity.master.tmd.NewsletterEvent;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.repository.master.tmd.NewsletterEventRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    public ResolvedBlockData resolve(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist) {
        int maxItems = resolveMaxItems(block);
        List<Map<String, Object>> items = newsletterEventRepository.findByNewsletterPublishHistoryOrderByEventCreateTime(pubHist).stream()
            .limit(maxItems)
            .map(this::toItem)
            .toList();

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("aggregationTime", chartSupport.calculateMarketTimes(pubHist.getPublishDateTime())[2] != null
            ? "뉴욕시간 전일 20:00부터 당일 17:00까지" : null);
        content.put("items", items);
        return ResolvedBlockData.of(content);
    }

    private int resolveMaxItems(TemplateTree.BlockNode block) {
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
            "content", List.of(event.getEventContent().split("\n")),
            "timestamp", event.getEventCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli(),
            "createdDate", event.getEventCreateTime().format(CREATED_DATE_FORMATTER),
            "markTime", event.getEventMarkTime(),
            "source", event.getEventProvider(),
            "isTurningPoint", "Y".equals(event.getTurningPointMarkYn()),
            "directionalityType", event.getDirectionalityType()
        );
    }
}
