package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.StaticBlockDefaultValueResolver;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.LinkedHashMap;
import java.util.List;
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
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        if (!executionPlan.isCanonical()) {
            return resolveLegacy(block, blockState, pubHist);
        }

        Map<String, Object> content = new LinkedHashMap<>(defaultValueResolver.resolve(block, pubHist));
        content.putAll(resolveStateBackedValues(block, blockState));
        return ResolvedBlockData.of(content);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveStateBackedValues(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState
    ) {
        if (blockState == null || blockState.state().isEmpty()) {
            return Map.of();
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        block.fields().forEach(field -> {
            Object directValue = blockState.state().get(field.fieldKey());
            if (directValue != null) {
                resolved.put(field.fieldKey(), directValue);
            }
        });

        Object contentInput = blockState.state().get("contentInput");
        if (contentInput instanceof Map<?, ?> inputMap) {
            resolved.putAll(new LinkedHashMap<>((Map<String, Object>) inputMap));
            return resolved;
        }
        if (contentInput instanceof List<?> inputList) {
            resolved.put("items", inputList);
        }
        return resolved;
    }

    private ResolvedBlockData resolveLegacy(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        Map<String, Object> contentInput = resolveStateBackedValues(block, blockState);
        if (!contentInput.isEmpty()) {
            return ResolvedBlockData.of(contentInput);
        }

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
