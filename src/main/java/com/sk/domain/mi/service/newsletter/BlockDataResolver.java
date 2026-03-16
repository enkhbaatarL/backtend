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
        Map<Long, IssueStateTree.BlockStateNode> blockStateMap = issueState.blockMap();
        for (TemplateTree.BlockNode block : template.allBlocks()) {
            BlockDataStrategy strategy = strategies.getOrDefault(block.dataSourceType(), strategies.get("STATIC"));
            try {
                result.put(block.blockCode(), strategy.resolve(block, blockStateMap.get(block.id()), pubHist));
            } catch (Exception e) {
                log.error("블록 resolve 실패 - blockCode: {}", block.blockCode(), e);
                result.put(block.blockCode(), ResolvedBlockData.error("BLOCK_RESOLVE_FAILED"));
            }
        }
        return result;
    }
}
