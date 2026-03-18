package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueBlockState;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueSectionState;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueBlockStateRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueSectionStateRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueStateLoader {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final NewsletterIssueSectionStateRepository sectionStateRepository;
    private final NewsletterIssueBlockStateRepository blockStateRepository;
    private final ObjectMapper objectMapper;

    public IssueStateTree load(Long issueOriginalId, TemplateTree template) {
        if (issueOriginalId == null) {
            return IssueStateTree.defaultFrom(template);
        }

        List<NewsletterIssueSectionState> sectionStates = sectionStateRepository
            .findByIssueOriginalIdAndIsDeletedFalseOrderBySectionOrder(issueOriginalId);

        if (sectionStates.isEmpty()) {
            return IssueStateTree.defaultFrom(template);
        }

        Map<Long, NewsletterIssueSectionState> sectionStateMap = new LinkedHashMap<>();
        sectionStates.forEach(section -> sectionStateMap.put(section.getTemplateSectionId(), section));

        List<Long> sectionStateIds = sectionStates.stream()
            .map(NewsletterIssueSectionState::getNewsletterIssueSectionStateId)
            .toList();

        Map<Long, List<NewsletterIssueBlockState>> blocksBySectionStateId = blockStateRepository
            .findBySectionStateIdInAndIsDeletedFalseOrderByBlockOrder(sectionStateIds)
            .stream()
            .collect(java.util.stream.Collectors.groupingBy(
                NewsletterIssueBlockState::getSectionStateId,
                LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));

        Map<Long, TemplateTree.SectionNode> templateSectionMap = template.sections().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.SectionNode::id, section -> section));
        Map<Long, TemplateTree.BlockNode> templateBlockMap = template.allBlocks().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.BlockNode::id, block -> block));

        List<IssueStateTree.SectionStateNode> effectiveSections = template.sections().stream()
            .map(section -> buildSectionState(section, sectionStateMap.get(section.id()), blocksBySectionStateId, templateBlockMap))
            .sorted(Comparator.comparingInt(IssueStateTree.SectionStateNode::order))
            .toList();

        validateSectionReferences(sectionStateMap, templateSectionMap);
        return new IssueStateTree(effectiveSections);
    }

    private IssueStateTree.SectionStateNode buildSectionState(
        TemplateTree.SectionNode templateSection,
        NewsletterIssueSectionState persistedSectionState,
        Map<Long, List<NewsletterIssueBlockState>> blocksBySectionStateId,
        Map<Long, TemplateTree.BlockNode> templateBlockMap
    ) {
        Map<String, Object> sectionStateJson = parseJson(persistedSectionState == null ? null : persistedSectionState.getSectionStateJson());
        List<NewsletterIssueBlockState> persistedBlocks = Optional.ofNullable(persistedSectionState)
            .map(NewsletterIssueSectionState::getNewsletterIssueSectionStateId)
            .map(blocksBySectionStateId::get)
            .orElse(List.of());

        validateBlockReferences(templateSection, persistedBlocks, templateBlockMap);

        Map<Long, List<NewsletterIssueBlockState>> persistedBlocksByTemplateBlockId = persistedBlocks.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                NewsletterIssueBlockState::getTemplateBlockId,
                LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));

        Set<Long> templateBlockIds = templateSection.blocks().stream()
            .map(TemplateTree.BlockNode::id)
            .collect(java.util.stream.Collectors.toSet());

        persistedBlocksByTemplateBlockId.keySet().stream()
            .filter(templateBlockId -> !templateBlockIds.contains(templateBlockId))
            .findFirst()
            .ifPresent(templateBlockId -> {
                throw new IllegalStateException("ISSUE_BLOCK_STATE_SECTION_MISMATCH: blockId=" + templateBlockId);
            });

        List<IssueStateTree.BlockStateNode> blocks = templateSection.blocks().stream()
            .flatMap(templateBlock -> {
                List<NewsletterIssueBlockState> matchedPersistedBlocks = persistedBlocksByTemplateBlockId.get(templateBlock.id());
                if (matchedPersistedBlocks == null || matchedPersistedBlocks.isEmpty()) {
                    return java.util.stream.Stream.of(IssueStateTree.BlockStateNode.defaultFrom(templateBlock));
                }
                return matchedPersistedBlocks.stream()
                    .map(persistedBlockState -> buildBlockState(templateSection, templateBlock, persistedBlockState, templateBlockMap));
            })
            .sorted(Comparator.comparingInt(IssueStateTree.BlockStateNode::order))
            .toList();

        return new IssueStateTree.SectionStateNode(
            persistedSectionState != null ? persistedSectionState.getNewsletterIssueSectionStateId() : null,
            templateSection.id(),
            persistedSectionState != null ? persistedSectionState.getSectionOrder() : templateSection.sectionOrder(),
            persistedSectionState != null ? persistedSectionState.getVisibilityYn() : templateSection.visibilityYn(),
            persistedSectionState != null && persistedSectionState.getSectionTitle() != null ? persistedSectionState.getSectionTitle() : templateSection.sectionName(),
            persistedSectionState != null && persistedSectionState.getTargetCompanyNo() != null ? persistedSectionState.getTargetCompanyNo() : templateSection.targetCompanyNo(),
            asBoolean(sectionStateJson.get("collapsed"), Boolean.FALSE),
            sectionStateJson,
            blocks
        );
    }

    private IssueStateTree.BlockStateNode buildBlockState(
        TemplateTree.SectionNode templateSection,
        TemplateTree.BlockNode templateBlock,
        NewsletterIssueBlockState persistedBlockState,
        Map<Long, TemplateTree.BlockNode> templateBlockMap
    ) {
        if (persistedBlockState == null) {
            return IssueStateTree.BlockStateNode.defaultFrom(templateBlock);
        }

        TemplateTree.BlockNode referencedBlock = templateBlockMap.get(persistedBlockState.getTemplateBlockId());
        if (referencedBlock == null) {
            throw new IllegalStateException("UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE: " + persistedBlockState.getTemplateBlockId());
        }
        if (!templateSection.id().equals(referencedBlock.sectionId())) {
            throw new IllegalStateException("ISSUE_BLOCK_STATE_SECTION_MISMATCH: blockId=" + referencedBlock.id());
        }

        Map<String, Object> blockStateJson = parseJson(persistedBlockState.getBlockStateJson());
        if (blockStateJson.containsKey("plotOnChart") && !"Y".equals(templateBlock.plotOnChartYn())) {
            throw new IllegalStateException("PLOT_ON_CHART_NOT_SUPPORTED: blockId=" + templateBlock.id());
        }

        return new IssueStateTree.BlockStateNode(
            persistedBlockState.getNewsletterIssueBlockStateId(),
            templateBlock.id(),
            persistedBlockState.getBlockOrder(),
            persistedBlockState.getVisibilityYn(),
            persistedBlockState.getBlockTitle() != null ? persistedBlockState.getBlockTitle() : templateBlock.blockName(),
            persistedBlockState.getTargetCompanyNo() != null ? persistedBlockState.getTargetCompanyNo() : templateBlock.targetCompanyNo(),
            asBoolean(blockStateJson.get("collapsed"), Boolean.FALSE),
            blockStateJson.containsKey("plotOnChart") ? asBoolean(blockStateJson.get("plotOnChart"), null) : null,
            blockStateJson
        );
    }

    private void validateBlockReferences(
        TemplateTree.SectionNode templateSection,
        List<NewsletterIssueBlockState> persistedBlocks,
        Map<Long, TemplateTree.BlockNode> templateBlockMap
    ) {
        persistedBlocks.forEach(persistedBlockState -> {
            TemplateTree.BlockNode referencedBlock = templateBlockMap.get(persistedBlockState.getTemplateBlockId());
            if (referencedBlock == null) {
                throw new IllegalStateException("UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE: " + persistedBlockState.getTemplateBlockId());
            }
            if (!templateSection.id().equals(referencedBlock.sectionId())) {
                throw new IllegalStateException("ISSUE_BLOCK_STATE_SECTION_MISMATCH: blockId=" + referencedBlock.id());
            }
        });
    }

    private void validateSectionReferences(
        Map<Long, NewsletterIssueSectionState> sectionStateMap,
        Map<Long, TemplateTree.SectionNode> templateSectionMap
    ) {
        sectionStateMap.keySet().stream()
            .filter(templateSectionId -> !templateSectionMap.containsKey(templateSectionId))
            .findFirst()
            .ifPresent(templateSectionId -> {
                throw new IllegalStateException("UNKNOWN_TEMPLATE_SECTION_STATE_REFERENCE: " + templateSectionId);
            });
    }

    private Map<String, Object> parseJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return normalizeStateMap(objectMapper.readValue(rawJson, MAP_TYPE));
        } catch (Exception e) {
            throw new IllegalStateException("ISSUE_STATE_JSON_PARSE_FAILED", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeStateMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = "showInChart".equals(key) ? "plotOnChart" : key;
            Object normalizedValue = value;
            if (value instanceof Map<?, ?> nestedMap) {
                normalizedValue = normalizeStateMap((Map<String, Object>) nestedMap);
            } else if (value instanceof List<?> listValue) {
                normalizedValue = listValue.stream()
                    .map(item -> item instanceof Map<?, ?> nestedItem
                        ? normalizeStateMap((Map<String, Object>) nestedItem)
                        : item)
                    .toList();
            }
            normalized.put(normalizedKey, normalizedValue);
        });
        return normalized;
    }

    private Boolean asBoolean(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
