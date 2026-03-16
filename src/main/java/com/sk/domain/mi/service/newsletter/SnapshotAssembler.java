package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.dto.snapshot.SnapshotBlock;
import com.sk.domain.mi.dto.snapshot.SnapshotMeta;
import com.sk.domain.mi.dto.snapshot.SnapshotRoot;
import com.sk.domain.mi.dto.snapshot.SnapshotSection;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotAssembler {

    private final ObjectMapper objectMapper;

    public String assemble(
        TemplateTree template,
        IssueStateTree issueState,
        Map<String, ResolvedBlockData> resolvedData,
        NewsletterPublishHistory pubHist
    ) {
        try {
            return assembleNode(template, issueState, resolvedData, pubHist).toString();
        } catch (Exception e) {
            throw new IllegalStateException("SNAPSHOT_ASSEMBLY_FAILED", e);
        }
    }

    public JsonNode assembleNode(
        TemplateTree template,
        IssueStateTree issueState,
        Map<String, ResolvedBlockData> resolvedData,
        NewsletterPublishHistory pubHist
    ) {
        SnapshotMeta meta = SnapshotMeta.builder()
            .title(pubHist.getNewsletterTitle())
            .emailSubject(pubHist.getEmailSubject())
            .aiSummary(pubHist.getAiSummary())
            .subInfoText(resolveSubInfoText(template, resolvedData))
            .build();

        Map<Long, IssueStateTree.SectionStateNode> sectionStateMap = issueState.sectionMap();
        Map<Long, IssueStateTree.BlockStateNode> blockStateMap = issueState.blockMap();
        List<SnapshotSection> sections = new ArrayList<>();
        for (TemplateTree.SectionNode sectionNode : template.sections()) {
            IssueStateTree.SectionStateNode sectionState = sectionStateMap.getOrDefault(
                sectionNode.id(),
                IssueStateTree.SectionStateNode.defaultFrom(sectionNode)
            );
            List<SnapshotBlock> blocks = new ArrayList<>();
            for (TemplateTree.BlockNode blockNode : sectionNode.blocks()) {
                IssueStateTree.BlockStateNode blockState = blockStateMap.getOrDefault(
                    blockNode.id(),
                    IssueStateTree.BlockStateNode.defaultFrom(blockNode)
                );
                blocks.add(SnapshotBlock.builder()
                    .id("blk-" + blockNode.blockCode())
                    .type(blockNode.blockTypeCode())
                    .format(blockNode.blockFormatCode())
                    .order(blockState.order())
                    .visibility("Y".equals(blockState.visibilityYn()))
                    .isCollapsed(Boolean.TRUE.equals(blockState.collapsed()))
                    .title(blockState.title())
                    .plotOnChart(resolvePlotOnChart(blockNode, blockState))
                    .target(resolveTarget(blockState.targetCompanyNo()))
                    .queryParams(blockState.queryParams().isEmpty() ? null : blockState.queryParams())
                    .state(blockState.extraState().isEmpty() ? null : blockState.extraState())
                    .editableFields(blockNode.fields().stream().filter(field -> "Y".equals(field.isEditableYn())).map(TemplateTree.FieldNode::fieldKey).toList())
                    .content(resolvedData.getOrDefault(blockNode.blockCode(), ResolvedBlockData.of(Map.of())).toContentMap())
                    .build());
            }
            sections.add(SnapshotSection.builder()
                .id("sec-" + sectionNode.sectionCode())
                .type(sectionNode.sectionTypeCode())
                .title(sectionState.title())
                .order(sectionState.order())
                .visibility("Y".equals(sectionState.visibilityYn()))
                .target(resolveTarget(sectionState.targetCompanyNo()))
                .isCollapsed(Boolean.TRUE.equals(sectionState.collapsed()))
                .state(sectionState.extraState().isEmpty() ? null : sectionState.extraState())
                .blocks(blocks)
                .build());
        }

        SnapshotRoot root = SnapshotRoot.builder()
            .schemaVersion(2)
            .templateId(template.template().id())
            .newsletterId(pubHist.getNewsletterMaster().getNewsletterMasterId())
            .issueNo(pubHist.getIssueNo())
            .publishedAt(pubHist.getPublishDateTime())
            .meta(meta)
            .sections(sections)
            .build();

        return objectMapper.valueToTree(root);
    }

    private String resolveSubInfoText(TemplateTree template, Map<String, ResolvedBlockData> resolvedData) {
        return template.allBlocks().stream()
            .filter(block -> "SUB_INFO_TEXT".equals(block.blockCode()))
            .findFirst()
            .map(block -> resolvedData.getOrDefault(block.blockCode(), ResolvedBlockData.of(Map.of())).toContentMap().get("text"))
            .map(Object::toString)
            .orElse(null);
    }

    private String resolveTarget(Integer targetCompanyNo) {
        return targetCompanyNo == null ? null : String.valueOf(targetCompanyNo);
    }

    private Boolean resolvePlotOnChart(TemplateTree.BlockNode blockNode, IssueStateTree.BlockStateNode blockState) {
        if (!"Y".equals(blockNode.plotOnChartYn())) {
            return null;
        }
        return blockState.plotOnChart() != null ? blockState.plotOnChart() : Boolean.FALSE;
    }
}
