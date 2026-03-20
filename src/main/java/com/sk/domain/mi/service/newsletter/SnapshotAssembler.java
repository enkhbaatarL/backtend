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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            .emailSubject(pubHist.getEmailSubject())
            .subInfoText(resolveSubInfoText(issueState, template, resolvedData))
            .build();

        Map<Long, TemplateTree.SectionNode> templateSectionMap = template.sections().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.SectionNode::id, section -> section, (left, right) -> left, LinkedHashMap::new));
        Map<Long, TemplateTree.BlockNode> templateBlockMap = template.allBlocks().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.BlockNode::id, block -> block, (left, right) -> left, LinkedHashMap::new));

        List<SnapshotSection> sections = new ArrayList<>();
        for (IssueStateTree.SectionStateNode sectionState : issueState.sections()) {
            TemplateTree.SectionNode sectionNode = templateSectionMap.get(sectionState.templateSectionId());
            if (sectionNode == null) {
                throw new IllegalStateException("UNKNOWN_TEMPLATE_SECTION_STATE_REFERENCE: " + sectionState.templateSectionId());
            }
            List<SnapshotBlock> blocks = new ArrayList<>();
            for (IssueStateTree.BlockStateNode blockState : sectionState.blocks()) {
                TemplateTree.BlockNode blockNode = templateBlockMap.get(blockState.templateBlockId());
                if (blockNode == null) {
                    throw new IllegalStateException("UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE: " + blockState.templateBlockId());
                }
                if ("NEWS_ITEM".equals(blockNode.blockCode())) {
                    blocks.addAll(expandNewsItemBlocks(blockNode, blockState, resolvedData));
                    continue;
                }
                blocks.add(SnapshotBlock.builder()
                    .id(resolveSnapshotBlockId(blockNode, blockState))
                    .type(blockNode.blockTypeCode())
                    .format(blockNode.blockFormatCode())
                    .order(blockState.order())
                    .visibility("Y".equals(blockState.visibilityYn()))
                    .title(blockState.title())
                    .plotOnChart(resolvePlotOnChart(blockNode, blockState))
                    .target(resolveTarget(blockState.targetCompanyNo()))
                    .queryParams(blockState.queryParams().isEmpty() ? null : blockState.queryParams())
                    .state(blockState.extraState().isEmpty() ? null : blockState.extraState())
                    .editableFields(blockNode.fields().stream().filter(field -> "Y".equals(field.isEditableYn())).map(TemplateTree.FieldNode::fieldKey).toList())
                    .content(resolvedData.getOrDefault(blockState.resolvedDataKey(), ResolvedBlockData.of(Map.of())).toContentMap())
                    .build());
            }
            sections.add(SnapshotSection.builder()
                .id("sec-" + sectionNode.sectionCode())
                .type(sectionNode.sectionTypeCode())
                .title(sectionState.title())
                .order(sectionState.order())
                .visibility("Y".equals(sectionState.visibilityYn()))
                .target(resolveTarget(sectionState.targetCompanyNo()))
                .state(sectionState.extraState().isEmpty() ? null : sectionState.extraState())
                .blocks(blocks)
                .build());
        }

        SnapshotRoot root = SnapshotRoot.builder()
            .meta(meta)
            .sections(sections)
            .build();

        return objectMapper.valueToTree(root);
    }

    private String resolveSubInfoText(
        IssueStateTree issueState,
        TemplateTree template,
        Map<String, ResolvedBlockData> resolvedData
    ) {
        Map<Long, TemplateTree.BlockNode> templateBlockMap = template.allBlocks().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.BlockNode::id, block -> block, (left, right) -> left));

        return issueState.sections().stream()
            .flatMap(sectionState -> sectionState.blocks().stream())
            .filter(blockState -> {
                TemplateTree.BlockNode blockNode = templateBlockMap.get(blockState.templateBlockId());
                return blockNode != null && "SUB_INFO_TEXT".equals(blockNode.blockCode());
            })
            .findFirst()
            .map(blockState -> resolvedData.getOrDefault(blockState.resolvedDataKey(), ResolvedBlockData.of(Map.of())).toContentMap().get("text"))
            .map(Object::toString)
            .orElse(null);
    }

    private String resolveSnapshotBlockId(TemplateTree.BlockNode blockNode, IssueStateTree.BlockStateNode blockState) {
        String baseId = "blk-" + blockNode.blockCode() + "-" + blockNode.id();
        return blockState.issueBlockStateId() != null
            ? baseId + "-" + blockState.issueBlockStateId()
            : baseId;
    }

    private List<SnapshotBlock> expandNewsItemBlocks(
        TemplateTree.BlockNode blockNode,
        IssueStateTree.BlockStateNode blockState,
        Map<String, ResolvedBlockData> resolvedData
    ) {
        ResolvedBlockData resolved = resolvedData.getOrDefault(blockState.resolvedDataKey(), ResolvedBlockData.of(Map.of()));
        List<Map<String, Object>> expandedItems = resolved.toExpandedContents();
        if (expandedItems.isEmpty()) {
            expandedItems = List.of(resolved.toContentMap());
        }

        List<String> editableFields = blockNode.fields().stream()
            .filter(field -> "Y".equals(field.isEditableYn()))
            .map(TemplateTree.FieldNode::fieldKey)
            .toList();

        List<SnapshotBlock> blocks = new ArrayList<>();
        for (int index = 0; index < expandedItems.size(); index++) {
            Map<String, Object> item = expandedItems.get(index);
            blocks.add(SnapshotBlock.builder()
                .id(resolveNewsItemSnapshotBlockId(item, index))
                .type(blockNode.blockTypeCode())
                .format(blockNode.blockFormatCode())
                // order는 0-based. NEWS_ITEM은 단일 블록이 복수 항목으로 확장되므로,
                // base order에 0-based index를 더해 각 항목의 순서를 부여한다.
                .order(blockState.order() == null ? index : blockState.order() + index)
                .visibility("Y".equals(blockState.visibilityYn()))
                .title(Objects.toString(item.get("title"), null))
                .plotOnChart(asBoolean(item.get("showInChart")))
                .target(resolveTarget(blockState.targetCompanyNo()))
                .queryParams(null)
                .state(new LinkedHashMap<>(Map.of("removed", Boolean.FALSE)))
                .editableFields(editableFields)
                .content(new LinkedHashMap<>(item))
                .build());
        }
        return blocks;
    }

    private String resolveNewsItemSnapshotBlockId(Map<String, Object> item, int index) {
        Object newsId = item.get("newsId");
        if (newsId != null && !String.valueOf(newsId).isBlank()) {
            return "blk-NEWS_ITEM-" + newsId;
        }
        boolean emptyItem = item.isEmpty() || (
            item.get("newsId") == null
                && item.get("title") == null
                && item.get("timestamp") == null
                && (item.get("content") instanceof List<?> contentList && contentList.isEmpty())
        );
        if (emptyItem) {
            return "blk-NEWS_ITEM-empty";
        }
        return "blk-NEWS_ITEM-idx-" + index;
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

    private Boolean asBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }
}
