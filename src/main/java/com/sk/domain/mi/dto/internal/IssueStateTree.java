package com.sk.domain.mi.dto.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record IssueStateTree(
    List<SectionStateNode> sections
) {

    public static IssueStateTree defaultFrom(TemplateTree template) {
        List<SectionStateNode> sectionStates = template.sections().stream()
            .map(SectionStateNode::defaultFrom)
            .toList();
        return new IssueStateTree(sectionStates);
    }

    private static Map<String, Object> filterState(Map<String, Object> source, List<String> excludedKeys) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        List<String> excluded = new ArrayList<>(excludedKeys);
        return source.entrySet().stream()
            .filter(entry -> !excluded.contains(entry.getKey()))
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> right,
                LinkedHashMap::new
            ));
    }

    private static Map<String, Object> includeState(Map<String, Object> source, List<String> includedKeys) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return source.entrySet().stream()
            .filter(entry -> includedKeys.contains(entry.getKey()))
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> right,
                LinkedHashMap::new
            ));
    }

    public Map<Long, SectionStateNode> sectionMap() {
        return sections.stream().collect(Collectors.toMap(SectionStateNode::templateSectionId, section -> section));
    }

    public Map<Long, BlockStateNode> blockMap() {
        Map<Long, BlockStateNode> blocks = new LinkedHashMap<>();
        sections.forEach(section -> section.blocks().forEach(block -> blocks.put(block.templateBlockId(), block)));
        return blocks;
    }

    public record SectionStateNode(
        Long issueSectionStateId,
        Long templateSectionId,
        Integer order,
        String visibilityYn,
        String title,
        Integer targetCompanyNo,
        Map<String, Object> state,
        List<BlockStateNode> blocks
    ) {

        public static SectionStateNode defaultFrom(TemplateTree.SectionNode section) {
            List<BlockStateNode> blockStates = section.blocks().stream()
                .map(BlockStateNode::defaultFrom)
                .toList();
            return new SectionStateNode(
                null,
                section.id(),
                section.sectionOrder(),
                section.visibilityYn(),
                section.sectionName(),
                section.targetCompanyNo(),
                Map.of(),
                blockStates
            );
        }

        public Map<String, Object> extraState() {
            return filterState(state, List.of());
        }
    }

    public record BlockStateNode(
        Long issueBlockStateId,
        Long templateBlockId,
        Integer order,
        String visibilityYn,
        String title,
        Integer targetCompanyNo,
        Boolean plotOnChart,
        Map<String, Object> state
    ) {

        public static BlockStateNode defaultFrom(TemplateTree.BlockNode block) {
            return new BlockStateNode(
                null,
                block.id(),
                block.blockOrder(),
                block.visibilityYn(),
                block.blockName(),
                block.targetCompanyNo(),
                null,
                Map.of()
            );
        }

        public String resolvedDataKey() {
            return issueBlockStateId != null ? "issue-block-state:" + issueBlockStateId : "template-block:" + templateBlockId;
        }

        public Map<String, Object> queryParams() {
            return includeState(state, List.of("fromDate", "toDate"));
        }

        public Map<String, Object> extraState() {
            return filterState(state, List.of("plotOnChart", "fromDate", "toDate"));
        }
    }
}
