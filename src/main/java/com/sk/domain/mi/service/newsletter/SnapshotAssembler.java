package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        Map<String, ResolvedBlockData> resolvedData,
        NewsletterPublishHistory pubHist
    ) {
        try {
            return assembleNode(template, resolvedData, pubHist).toString();
        } catch (Exception e) {
            throw new IllegalStateException("SNAPSHOT_ASSEMBLY_FAILED", e);
        }
    }

    public JsonNode assembleNode(
        TemplateTree template,
        Map<String, ResolvedBlockData> resolvedData,
        NewsletterPublishHistory pubHist
    ) {
        SnapshotMeta meta = SnapshotMeta.builder()
            .emailSubject(pubHist.getEmailSubject())
            .subInfoText(resolveSubInfoText(template, resolvedData))
            .build();

        List<SnapshotSection> sections = new ArrayList<>();
        for (TemplateTree.SectionNode sectionNode : template.sections()) {
            List<SnapshotBlock> blocks = new ArrayList<>();
            for (TemplateTree.BlockNode blockNode : sectionNode.blocks()) {
                if ("NEWS_ITEM".equals(blockNode.blockCode())) {
                    blocks.addAll(expandNewsItemBlocks(blockNode, resolvedData));
                    continue;
                }
                blocks.add(SnapshotBlock.builder()
                    .id(resolveSnapshotBlockId(blockNode))
                    .type(blockNode.blockTypeCode())
                    .format(blockNode.blockFormatCode())
                    .order(blockNode.blockOrder())
                    .visibility("Y".equals(blockNode.visibilityYn()))
                    .title(blockNode.blockName())
                    .plotOnChart(resolvePlotOnChart(blockNode))
                    .target(resolveTarget(blockNode.targetCompanyNo()))
                    .queryParams(null)
                    .state(null)
                    .editableFields(blockNode.fields().stream().filter(field -> "Y".equals(field.isEditableYn())).map(TemplateTree.FieldNode::fieldKey).toList())
                    .content(resolvedData.getOrDefault(BlockDataResolver.resolvedDataKey(blockNode.id()), ResolvedBlockData.of(Map.of())).toContentMap())
                    .build());
            }
            sections.add(SnapshotSection.builder()
                .id("sec-" + sectionNode.sectionCode())
                .type(sectionNode.sectionTypeCode())
                .title(sectionNode.sectionName())
                .order(sectionNode.sectionOrder())
                .visibility("Y".equals(sectionNode.visibilityYn()))
                .target(resolveTarget(sectionNode.targetCompanyNo()))
                .state(defaultSectionState())
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
        TemplateTree template,
        Map<String, ResolvedBlockData> resolvedData
    ) {
        return template.allBlocks().stream()
            .filter(blockNode -> "SUB_INFO_TEXT".equals(blockNode.blockCode()))
            .findFirst()
            .map(blockNode -> resolvedData.getOrDefault(BlockDataResolver.resolvedDataKey(blockNode.id()), ResolvedBlockData.of(Map.of())).toContentMap().get("text"))
            .map(Object::toString)
            .orElse(null);
    }

    private String resolveSnapshotBlockId(TemplateTree.BlockNode blockNode) {
        return "blk-" + blockNode.blockCode() + "-" + blockNode.id();
    }

    private List<SnapshotBlock> expandNewsItemBlocks(
        TemplateTree.BlockNode blockNode,
        Map<String, ResolvedBlockData> resolvedData
    ) {
        ResolvedBlockData resolved = resolvedData.getOrDefault(BlockDataResolver.resolvedDataKey(blockNode.id()), ResolvedBlockData.of(Map.of()));
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
                .order(blockNode.blockOrder() == null ? index : blockNode.blockOrder() + index)
                .visibility("Y".equals(blockNode.visibilityYn()))
                .title(Objects.toString(item.get("title"), null))
                .plotOnChart(asBoolean(item.get("showInChart")))
                .target(resolveTarget(blockNode.targetCompanyNo()))
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

    private Map<String, Object> defaultSectionState() {
        return new LinkedHashMap<>(Map.of("removed", Boolean.FALSE));
    }

    private Boolean resolvePlotOnChart(TemplateTree.BlockNode blockNode) {
        if (!"Y".equals(blockNode.plotOnChartYn())) {
            return null;
        }
        return Boolean.FALSE;
    }

    private Boolean asBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }
}
