package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("STATIC")
public class StaticBlockDataStrategy implements BlockDataStrategy {

    private static final DateTimeFormatter TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    @Override
    public String getType() {
        return "STATIC";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        Map<String, Object> contentInput = resolveContentInput(blockState);
        if (!contentInput.isEmpty()) {
            return ResolvedBlockData.of(contentInput);
        }

        Map<String, Object> content = new LinkedHashMap<>();
        if ("TITLE".equals(block.blockTypeCode())) {
            content.put("text", pubHist != null ? pubHist.getNewsletterTitle() : block.blockName());
            content.put("publishDate", pubHist != null && pubHist.getPublishDateTime() != null
                ? pubHist.getPublishDateTime().format(TITLE_DATE_FORMATTER) : null);
            content.put("issueLabel", pubHist != null ? "#" + pubHist.getIssueNo() : null);
            return ResolvedBlockData.of(content);
        }

        if ("AI_SUMMARY_TEXT".equals(block.blockCode())) {
            content.put("text", pubHist != null ? pubHist.getAiSummary() : null);
            return ResolvedBlockData.of(content);
        }

        String text = block.fields().stream()
            .filter(field -> "text".equals(field.fieldKey()) || "title".equals(field.fieldKey()))
            .map(field -> {
                if (field.colMapping() != null && field.colMapping().startsWith("STATIC:")) {
                    return field.colMapping().substring("STATIC:".length());
                }
                return block.blockName();
            })
            .findFirst()
            .orElse(block.blockName());
        content.put("text", text);
        return ResolvedBlockData.of(content);
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
        if (contentInput instanceof List<?> inputList) {
            return Map.of("items", inputList);
        }
        return Map.of();
    }
}
