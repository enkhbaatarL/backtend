package com.sk.domain.mi.service.newsletter;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.service.newsletter.strategy.BlockDataStrategy;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockDataResolver {

    private final Map<String, BlockDataStrategy> strategies;
    private final BlockExecutionClassifier executionClassifier;

    public static String resolvedDataKey(Long templateBlockId) {
        return "template-block:" + templateBlockId;
    }

    public Map<String, ResolvedBlockData> resolveAll(
        TemplateTree template,
        NewsletterPublishHistory pubHist
    ) {
        return resolveAll(template, pubHist, true);
    }

    public Map<String, ResolvedBlockData> resolveAll(
        TemplateTree template,
        NewsletterPublishHistory pubHist,
        boolean validateResolvedContent
    ) {
        Map<String, ResolvedBlockData> result = new LinkedHashMap<>();
        template.sections().forEach(section -> section.blocks().forEach(block -> {
            BlockExecutionPlan executionPlan = executionClassifier.plan(block);
            executionClassifier.validateOrThrow(block, executionPlan);
            BlockDataStrategy strategy = strategies.getOrDefault(block.dataSourceType(), strategies.get("STATIC"));
            try {
                ResolvedBlockData resolved = strategy.resolve(block, executionPlan, pubHist);
                if (validateResolvedContent) {
                    executionClassifier.validateResolvedContent(block, executionPlan, resolved);
                }
                result.put(resolvedDataKey(block.id()), resolved);
            } catch (AppErrorException e) {
                throw e;
            } catch (Exception e) {
                log.error("블록 resolve 실패 - blockCode: {}, templateBlockId: {}", block.blockCode(), block.id(), e);
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "BLOCK_RESOLVE_FAILED:" + block.blockCode());
            }
        }));
        return result;
    }
}
