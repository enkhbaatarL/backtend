package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.StaticBlockDefaultValueResolver;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("STATIC")
@RequiredArgsConstructor
public class StaticBlockDataStrategy implements BlockDataStrategy {

    private final StaticBlockDefaultValueResolver defaultValueResolver;

    @Override
    public String getType() {
        return "STATIC";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        BlockExecutionPlan executionPlan,
        NewsletterPublishHistory pubHist
    ) {
        if (!executionPlan.isCanonical()) {
            return resolveLegacy(block, pubHist);
        }

        Map<String, Object> content = new LinkedHashMap<>(defaultValueResolver.resolve(block, pubHist));
        return ResolvedBlockData.of(content);
    }

    private ResolvedBlockData resolveLegacy(
        TemplateTree.BlockNode block,
        NewsletterPublishHistory pubHist
    ) {
        Map<String, Object> content = new LinkedHashMap<>();
        if ("TITLE".equals(block.blockTypeCode())) {
            content.put("text", pubHist != null ? pubHist.getNewsletterTitle() : block.blockName());
            content.put("publishDate", pubHist != null && pubHist.getPublishDateTime() != null
                ? pubHist.getPublishDateTime().toLocalDate().toString() : null);
            content.put("issueLabel", pubHist != null ? "#" + pubHist.getIssueNo() : null);
            return ResolvedBlockData.of(content);
        }

        if ("AI_SUMMARY_TEXT".equals(block.blockCode())) {
            content.put("text", pubHist != null ? pubHist.getAiSummary() : null);
            return ResolvedBlockData.of(content);
        }

        String text = block.fields().stream()
            .filter(field -> "text".equals(field.fieldKey()) || "title".equals(field.fieldKey()))
            .map(field -> field.colMapping() != null && field.colMapping().startsWith("STATIC:")
                ? field.colMapping().substring("STATIC:".length())
                : block.blockName())
            .findFirst()
            .orElse(block.blockName());
        content.put("text", text);
        return ResolvedBlockData.of(content);
    }
}
