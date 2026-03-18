package com.sk.domain.mi.service.newsletter;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.IssueStateTree;
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

    public Map<String, ResolvedBlockData> resolveAll(
        TemplateTree template,
        IssueStateTree issueState,
        NewsletterPublishHistory pubHist
    ) {
        return resolveAll(template, issueState, pubHist, true);
    }

    public Map<String, ResolvedBlockData> resolveAll(
        TemplateTree template,
        IssueStateTree issueState,
        NewsletterPublishHistory pubHist,
        boolean validateResolvedContent
    ) {
        Map<String, ResolvedBlockData> result = new LinkedHashMap<>();
        Map<Long, TemplateTree.BlockNode> templateBlockMap = template.allBlocks().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.BlockNode::id, block -> block));

        issueState.sections().forEach(sectionState -> sectionState.blocks().forEach(blockState -> {
            TemplateTree.BlockNode block = templateBlockMap.get(blockState.templateBlockId());
            if (block == null) {
                throw new IllegalStateException("UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE: " + blockState.templateBlockId());
            }

            BlockExecutionPlan executionPlan = executionClassifier.plan(block, blockState);
            executionClassifier.validateOrThrow(block, executionPlan);
            BlockDataStrategy strategy = strategies.getOrDefault(block.dataSourceType(), strategies.get("STATIC"));
            try {
                ResolvedBlockData resolved = strategy.resolve(block, executionPlan, blockState, pubHist);
                if (validateResolvedContent) {
                    executionClassifier.validateResolvedContent(block, executionPlan, resolved.toContentMap());
                }
                result.put(blockState.resolvedDataKey(), resolved);
            } catch (AppErrorException e) {
                throw e;
            } catch (Exception e) {
                log.error("블록 resolve 실패 - blockCode: {}, issueBlockStateId: {}", block.blockCode(), blockState.issueBlockStateId(), e);
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "BLOCK_RESOLVE_FAILED:" + block.blockCode());
            }
        }));
        return result;
    }
}
