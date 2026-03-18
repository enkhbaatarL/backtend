package com.sk.domain.mi.service.newsletter;

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

    public Map<String, ResolvedBlockData> resolveAll(
        TemplateTree template,
        IssueStateTree issueState,
        NewsletterPublishHistory pubHist
    ) {
        Map<String, ResolvedBlockData> result = new LinkedHashMap<>();
        Map<Long, TemplateTree.BlockNode> templateBlockMap = template.allBlocks().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.BlockNode::id, block -> block));

        issueState.sections().forEach(sectionState -> sectionState.blocks().forEach(blockState -> {
            TemplateTree.BlockNode block = templateBlockMap.get(blockState.templateBlockId());
            if (block == null) {
                throw new IllegalStateException("UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE: " + blockState.templateBlockId());
            }

            BlockDataStrategy strategy = strategies.getOrDefault(block.dataSourceType(), strategies.get("STATIC"));
            try {
                result.put(blockState.resolvedDataKey(), strategy.resolve(block, blockState, pubHist));
            } catch (Exception e) {
                log.error("블록 resolve 실패 - blockCode: {}, issueBlockStateId: {}", block.blockCode(), blockState.issueBlockStateId(), e);
                result.put(blockState.resolvedDataKey(), ResolvedBlockData.error("BLOCK_RESOLVE_FAILED"));
            }
        }));
        return result;
    }
}
