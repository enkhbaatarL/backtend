package com.sk.domain.mi.service.newsletter;

import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaticBlockDefaultValueResolver {

    private final TemplateFieldValueResolver fieldValueResolver;

    public Map<String, Object> resolve(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist) {
        Map<String, Object> content = new LinkedHashMap<>();

        for (TemplateTree.FieldNode field : block.fields()) {
            Object resolvedValue = fieldValueResolver.resolveBlockFieldValue(block, field, pubHist);
            if (resolvedValue != null) {
                content.put(field.fieldKey(), resolvedValue);
            }
        }

        return content;
    }
}
