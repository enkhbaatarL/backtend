package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.dto.snapshot.SnapshotBlock;
import com.sk.domain.mi.dto.snapshot.SnapshotFieldDef;
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
    private final TemplateFieldValueResolver fieldValueResolver;

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
            LinkedHashMap<String, Object> sectionFields = buildSectionFields(sectionNode, resolvedData, pubHist);
            LinkedHashMap<String, SnapshotFieldDef> sectionFieldDefs = buildSectionFieldDefs(sectionNode);
            for (TemplateTree.BlockNode blockNode : sectionNode.blocks()) {
                if ("NEWS_ITEM".equals(blockNode.blockCode())) {
                    blocks.addAll(expandNewsItemBlocks(blockNode, resolvedData));
                    continue;
                }
                if (shouldSkipBlock(sectionNode.sectionCode(), blockNode.blockCode())) {
                    continue;
                }

                Map<String, Object> content = resolvedData
                    .getOrDefault(BlockDataResolver.resolvedDataKey(blockNode.id()), ResolvedBlockData.of(Map.of()))
                    .toContentMap();
                LinkedHashMap<String, Object> blockFields = content.isEmpty() ? null : new LinkedHashMap<>(content);
                LinkedHashMap<String, SnapshotFieldDef> blockFieldDefs = buildBlockFieldDefs(blockNode);

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
                    .fields(blockFields)
                    .fieldDefs(blockFieldDefs.isEmpty() ? null : blockFieldDefs)
                    .build());
            }
            sections.add(SnapshotSection.builder()
                .id("sec-" + sectionNode.sectionCode() + "-" + sectionNode.id())
                .type(sectionNode.sectionTypeCode())
                .code(sectionNode.sectionCode())
                .name(sectionNode.sectionName())
                .order(sectionNode.sectionOrder())
                .visibility("Y".equals(sectionNode.visibilityYn()))
                .target(resolveTarget(sectionNode.targetCompanyNo()))
                .removed(Boolean.FALSE)
                .fields(sectionFields.isEmpty() ? null : sectionFields)
                .fieldDefs(sectionFieldDefs.isEmpty() ? null : sectionFieldDefs)
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

        LinkedHashMap<String, SnapshotFieldDef> blockFieldDefs = buildBlockFieldDefs(blockNode);

        List<SnapshotBlock> blocks = new ArrayList<>();
        for (int index = 0; index < expandedItems.size(); index++) {
            Map<String, Object> item = expandedItems.get(index);
            LinkedHashMap<String, Object> blockFields = item.isEmpty() ? null : new LinkedHashMap<>(item);
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
                .fields(blockFields)
                .fieldDefs(blockFieldDefs.isEmpty() ? null : new LinkedHashMap<>(blockFieldDefs))
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

    private Boolean resolvePlotOnChart(TemplateTree.BlockNode blockNode) {
        if (!"Y".equals(blockNode.plotOnChartYn())) {
            return null;
        }
        return Boolean.FALSE;
    }

    private Boolean asBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    private boolean shouldSkipBlock(String sectionCode, String blockCode) {
        return isReferenceSection(sectionCode) && "REFERENCE_HEADER".equals(blockCode);
    }

    private LinkedHashMap<String, Object> buildSectionFields(
        TemplateTree.SectionNode sectionNode,
        Map<String, ResolvedBlockData> resolvedData,
        NewsletterPublishHistory pubHist
    ) {
        LinkedHashMap<String, Object> sectionFields = new LinkedHashMap<>();
        for (TemplateTree.SectionFieldNode fieldNode : sectionNode.sectionFields()) {
            Object resolvedValue = fieldValueResolver.resolveSectionFieldValue(sectionNode, fieldNode, resolvedData, pubHist);
            if (resolvedValue != null) {
                sectionFields.put(fieldNode.fieldKey(), resolvedValue);
            }
        }
        return sectionFields;
    }

    private LinkedHashMap<String, SnapshotFieldDef> buildSectionFieldDefs(TemplateTree.SectionNode sectionNode) {
        LinkedHashMap<String, SnapshotFieldDef> sectionFieldDefs = new LinkedHashMap<>();
        for (TemplateTree.SectionFieldNode fieldNode : sectionNode.sectionFields()) {
            sectionFieldDefs.put(fieldNode.fieldKey(), buildFieldDef(fieldNode.isEditableYn(), fieldNode.isRequiredYn()));
        }
        return sectionFieldDefs;
    }

    private LinkedHashMap<String, SnapshotFieldDef> buildBlockFieldDefs(TemplateTree.BlockNode blockNode) {
        LinkedHashMap<String, SnapshotFieldDef> blockFieldDefs = new LinkedHashMap<>();
        for (TemplateTree.FieldNode fieldNode : blockNode.fields()) {
            blockFieldDefs.put(fieldNode.fieldKey(), buildFieldDef(fieldNode.isEditableYn(), fieldNode.isRequiredYn()));
        }
        return blockFieldDefs;
    }

    private SnapshotFieldDef buildFieldDef(String editableYn, String requiredYn) {
        return SnapshotFieldDef.builder()
            .editable("Y".equals(editableYn))
            .required("Y".equals(requiredYn))
            .build();
    }

    private boolean isReferenceSection(String sectionCode) {
        return "REFERENCE".equals(sectionCode);
    }
}
