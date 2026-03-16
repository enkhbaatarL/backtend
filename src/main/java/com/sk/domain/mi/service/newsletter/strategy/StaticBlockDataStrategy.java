package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
    public ResolvedBlockData resolve(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist) {
        Map<String, Object> content = new LinkedHashMap<>();
        if ("TITLE".equals(block.blockTypeCode())) {
            content.put("text", pubHist.getNewsletterTitle());
            content.put("publishDate", pubHist.getPublishDateTime() == null ? null : pubHist.getPublishDateTime().format(TITLE_DATE_FORMATTER));
            content.put("issueLabel", "#" + pubHist.getIssueNo());
            return ResolvedBlockData.of(content);
        }

        if ("AI_SUMMARY_TEXT".equals(block.blockCode())) {
            content.put("text", pubHist.getAiSummary());
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
}
