package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.dto.request.newsletter.NewsletterIssueStateSaveReq;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssuePreviewRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueStateSaveRes;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueBlockState;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueSectionState;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueBlockStateRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueSectionStateRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterIssuePreviewWriteService {

    private static final Set<String> FIXED_SECTION_CODES = Set.of(
        "COVER", "DATE_ISSUE", "TITLE_SPECIALIZED", "SUMMARY", "NEWS_FLOW",
        "REFERENCE1", "REFERENCE2", "REFERENCE3", "REFERENCE4", "FOOTER"
    );
    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterIssueSectionStateRepository sectionStateRepository;
    private final NewsletterIssueBlockStateRepository blockStateRepository;
    private final TemplateLoader templateLoader;
    private final IssueStateLoader issueStateLoader;
    private final BlockDataResolver blockDataResolver;
    private final SnapshotAssembler snapshotAssembler;
    private final ObjectMapper objectMapper;

    public NewsletterIssuePreviewRes getPreview(Integer newsletterId, Integer issueNo, User actorUser) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(newsletterId, issueNo, actorUser);
        TemplateTree template = templateLoader.load(newsletterId);
        IssueStateTree issueState = issueStateLoader.load(issueOriginal.getNewsletterIssueOriginalId(), template);
        Map<String, ResolvedBlockData> resolvedData = blockDataResolver.resolveAll(
            template,
            issueState,
            issueOriginal.getSourceNewsletterPublishHistory(),
            false
        );

        return new NewsletterIssuePreviewRes(
            newsletterId,
            issueNo,
            2,
            "PREVIEW",
            snapshotAssembler.assembleNode(template, issueState, resolvedData, issueOriginal.getSourceNewsletterPublishHistory())
        );
    }

    @Transactional
    public NewsletterIssueStateSaveRes saveIssueState(
        Integer newsletterId,
        Integer issueNo,
        NewsletterIssueStateSaveReq request,
        User actorUser
    ) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(newsletterId, issueNo, actorUser);
        TemplateTree template = templateLoader.load(newsletterId);
        NewsletterIssueStateSaveReq normalizedRequest = normalizeRequest(request);

        validateTemplateSections(template);
        validateRequest(template, normalizedRequest);

        List<NewsletterIssueSectionState> existingSections = sectionStateRepository
            .findByIssueOriginalIdAndIsDeletedFalseOrderBySectionOrder(issueOriginal.getNewsletterIssueOriginalId());
        Map<Long, NewsletterIssueSectionState> existingSectionsById = existingSections.stream()
            .collect(java.util.stream.Collectors.toMap(
                NewsletterIssueSectionState::getNewsletterIssueSectionStateId,
                section -> section,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<Long, NewsletterIssueSectionState> existingSectionsByTemplateId = existingSections.stream()
            .collect(java.util.stream.Collectors.toMap(
                NewsletterIssueSectionState::getTemplateSectionId,
                section -> section,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        List<NewsletterIssueSectionState> sectionStatesToSave = new ArrayList<>();
        Map<Long, NewsletterIssueSectionState> savedSectionByTemplateId = new LinkedHashMap<>();
        for (NewsletterIssueStateSaveReq.Section requestedSection : normalizedRequest.sections()) {
            NewsletterIssueSectionState sectionState = resolveSectionState(
                issueOriginal.getNewsletterIssueOriginalId(),
                requestedSection,
                existingSectionsById,
                existingSectionsByTemplateId
            );
            sectionState.applyState(
                requestedSection.order(),
                toYn(requestedSection.visibility()),
                requestedSection.title(),
                requestedSection.targetCompanyNo(),
                writeJson(buildSectionStateJson(requestedSection))
            );
            sectionStatesToSave.add(sectionState);
        }
        List<NewsletterIssueSectionState> savedSections = sectionStateRepository.saveAll(sectionStatesToSave);
        savedSections.forEach(section -> savedSectionByTemplateId.put(section.getTemplateSectionId(), section));

        List<Long> savedSectionIds = savedSections.stream()
            .map(NewsletterIssueSectionState::getNewsletterIssueSectionStateId)
            .toList();
        List<NewsletterIssueBlockState> existingBlocks = savedSectionIds.isEmpty()
            ? List.of()
            : blockStateRepository.findBySectionStateIdInAndIsDeletedFalseOrderByBlockOrder(savedSectionIds);
        Map<Long, NewsletterIssueBlockState> existingBlocksById = existingBlocks.stream()
            .collect(java.util.stream.Collectors.toMap(
                NewsletterIssueBlockState::getNewsletterIssueBlockStateId,
                block -> block,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<Long, List<NewsletterIssueBlockState>> existingBlocksBySectionId = existingBlocks.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                NewsletterIssueBlockState::getSectionStateId,
                LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));

        List<NewsletterIssueBlockState> blockStatesToSave = new ArrayList<>();
        for (NewsletterIssueStateSaveReq.Section requestedSection : normalizedRequest.sections()) {
            NewsletterIssueSectionState savedSection = savedSectionByTemplateId.get(requestedSection.templateSectionId());
            if (savedSection == null) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "SECTION_STATE_SAVE_FAILED");
            }

            Set<Long> retainedBlockIds = new LinkedHashSet<>();
            for (NewsletterIssueStateSaveReq.Block requestedBlock : requestedSection.blocks()) {
                NewsletterIssueBlockState blockState = resolveBlockState(
                    issueOriginal.getNewsletterIssueOriginalId(),
                    savedSection.getNewsletterIssueSectionStateId(),
                    requestedBlock,
                    existingBlocksById
                );
                blockState.applyState(
                    savedSection.getNewsletterIssueSectionStateId(),
                    requestedBlock.order(),
                    toYn(requestedBlock.visibility()),
                    requestedBlock.title(),
                    requestedBlock.targetCompanyNo(),
                    writeJson(buildBlockStateJson(requestedBlock))
                );
                blockStatesToSave.add(blockState);
                if (blockState.getNewsletterIssueBlockStateId() != null) {
                    retainedBlockIds.add(blockState.getNewsletterIssueBlockStateId());
                }
            }

            existingBlocksBySectionId.getOrDefault(savedSection.getNewsletterIssueSectionStateId(), List.of()).stream()
                .filter(existingBlock -> !retainedBlockIds.contains(existingBlock.getNewsletterIssueBlockStateId()))
                .forEach(existingBlock -> {
                    existingBlock.softDelete();
                    blockStatesToSave.add(existingBlock);
                });
        }
        blockStateRepository.saveAll(blockStatesToSave);

        NewsletterIssuePreviewRes preview = getPreview(newsletterId, issueNo, actorUser);
        return new NewsletterIssueStateSaveRes(
            newsletterId,
            issueNo,
            issueOriginal.getNewsletterIssueOriginalId(),
            savedSections.size(),
            (int) blockStatesToSave.stream().filter(block -> !Boolean.TRUE.equals(block.getIsDeleted())).count(),
            preview
        );
    }

    private NewsletterIssueOriginal getAccessibleIssue(Integer newsletterId, Integer issueNo, User actorUser) {
        Integer customerCompanyNo = actorUser != null && actorUser.getCustomerCompany() != null
            ? actorUser.getCustomerCompany().getCustomerCompanyNo() : null;
        return issueOriginalRepository.findAccessibleIssue(newsletterId, issueNo, customerCompanyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));
    }

    private NewsletterIssueSectionState resolveSectionState(
        Long issueOriginalId,
        NewsletterIssueStateSaveReq.Section requestedSection,
        Map<Long, NewsletterIssueSectionState> existingSectionsById,
        Map<Long, NewsletterIssueSectionState> existingSectionsByTemplateId
    ) {
        NewsletterIssueSectionState sectionState = requestedSection.issueSectionStateId() != null
            ? existingSectionsById.get(requestedSection.issueSectionStateId())
            : existingSectionsByTemplateId.get(requestedSection.templateSectionId());

        if (sectionState != null) {
            if (!Objects.equals(sectionState.getIssueOriginalId(), issueOriginalId)) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_SECTION_STATE_MISMATCH");
            }
            return sectionState;
        }

        return NewsletterIssueSectionState.create(
            issueOriginalId,
            requestedSection.templateSectionId(),
            requestedSection.order(),
            toYn(requestedSection.visibility()),
            requestedSection.title(),
            requestedSection.targetCompanyNo(),
            writeJson(buildSectionStateJson(requestedSection))
        );
    }

    private NewsletterIssueBlockState resolveBlockState(
        Long issueOriginalId,
        Long sectionStateId,
        NewsletterIssueStateSaveReq.Block requestedBlock,
        Map<Long, NewsletterIssueBlockState> existingBlocksById
    ) {
        NewsletterIssueBlockState blockState = requestedBlock.issueBlockStateId() != null
            ? existingBlocksById.get(requestedBlock.issueBlockStateId())
            : null;

        if (blockState != null) {
            if (!Objects.equals(blockState.getIssueOriginalId(), issueOriginalId)) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_BLOCK_STATE_MISMATCH");
            }
            return blockState;
        }

        return NewsletterIssueBlockState.create(
            issueOriginalId,
            sectionStateId,
            requestedBlock.templateBlockId(),
            requestedBlock.order(),
            toYn(requestedBlock.visibility()),
            requestedBlock.title(),
            requestedBlock.targetCompanyNo(),
            writeJson(buildBlockStateJson(requestedBlock))
        );
    }

    private NewsletterIssueStateSaveReq normalizeRequest(NewsletterIssueStateSaveReq request) {
        if (request == null || request.sections() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "ISSUE_STATE_REQUEST_REQUIRED");
        }

        List<NewsletterIssueStateSaveReq.Section> normalizedSections = request.sections().stream()
            .map(section -> new NewsletterIssueStateSaveReq.Section(
                section.issueSectionStateId(),
                section.templateSectionId(),
                section.order(),
                section.visibility() == null ? Boolean.TRUE : section.visibility(),
                StringUtils.trimToNull(section.title()),
                section.targetCompanyNo(),
                normalizeSectionStateMap(section.state()),
                section.blocks() == null ? List.of() : section.blocks().stream()
                    .map(block -> new NewsletterIssueStateSaveReq.Block(
                        block.issueBlockStateId(),
                        block.templateBlockId(),
                        block.order(),
                        block.visibility() == null ? Boolean.TRUE : block.visibility(),
                        StringUtils.trimToNull(block.title()),
                        block.targetCompanyNo(),
                        block.plotOnChart(),
                        normalizeStateMap(block.state())
                    ))
                    .toList()
            ))
            .toList();

        return new NewsletterIssueStateSaveReq(normalizedSections);
    }

    private void validateTemplateSections(TemplateTree template) {
        Set<String> sectionCodes = template.sections().stream()
            .map(TemplateTree.SectionNode::sectionCode)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!sectionCodes.equals(FIXED_SECTION_CODES)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_STATE_TEMPLATE_STRUCTURE_MISMATCH");
        }
    }

    private void validateRequest(TemplateTree template, NewsletterIssueStateSaveReq request) {
        Map<Long, TemplateTree.SectionNode> templateSectionMap = template.sections().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.SectionNode::id, section -> section, (left, right) -> left, LinkedHashMap::new));
        Map<Long, TemplateTree.BlockNode> templateBlockMap = template.allBlocks().stream()
            .collect(java.util.stream.Collectors.toMap(TemplateTree.BlockNode::id, block -> block, (left, right) -> left, LinkedHashMap::new));

        if (request.sections().size() != template.sections().size()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_STATE_SECTION_COUNT_MISMATCH");
        }

        Set<Long> seenSectionTemplateIds = new LinkedHashSet<>();
        Set<Integer> seenSectionOrders = new LinkedHashSet<>();
        for (NewsletterIssueStateSaveReq.Section section : request.sections()) {
            TemplateTree.SectionNode templateSection = templateSectionMap.get(section.templateSectionId());
            if (templateSection == null) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "UNKNOWN_TEMPLATE_SECTION_STATE_REFERENCE");
            }
            if (!seenSectionTemplateIds.add(section.templateSectionId())) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "DUPLICATE_TEMPLATE_SECTION_STATE");
            }
            if (section.order() == null || !seenSectionOrders.add(section.order())) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "DUPLICATE_SECTION_ORDER");
            }
            if (section.blocks() == null || section.blocks().isEmpty()) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_STATE_BLOCK_REQUIRED");
            }

            Set<Integer> seenBlockOrders = new LinkedHashSet<>();
            Map<Long, Integer> blockCountByTemplateId = new LinkedHashMap<>();
            for (NewsletterIssueStateSaveReq.Block block : section.blocks()) {
                TemplateTree.BlockNode templateBlock = templateBlockMap.get(block.templateBlockId());
                if (templateBlock == null) {
                    throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE");
                }
                if (!Objects.equals(templateBlock.sectionId(), section.templateSectionId())) {
                    throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_BLOCK_STATE_SECTION_MISMATCH");
                }
                if ("NEWS_ITEM".equals(templateBlock.blockCode())) {
                    throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "NEWS_ITEM_STATE_NOT_SUPPORTED");
                }
                if (block.order() == null || !seenBlockOrders.add(block.order())) {
                    throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "DUPLICATE_BLOCK_ORDER");
                }

                int nextCount = blockCountByTemplateId.getOrDefault(block.templateBlockId(), 0) + 1;
                blockCountByTemplateId.put(block.templateBlockId(), nextCount);
                if (nextCount > 1) {
                    throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "NON_REPEATABLE_BLOCK_DUPLICATED");
                }

                Map<String, Object> normalizedState = normalizeStateMap(block.state());
                if (normalizedState.containsKey("plotOnChart") && !"Y".equals(templateBlock.plotOnChartYn())) {
                    throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "PLOT_ON_CHART_NOT_SUPPORTED");
                }
            }
        }

        if (!seenSectionTemplateIds.equals(templateSectionMap.keySet())) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "ISSUE_STATE_SECTION_SET_MISMATCH");
        }
    }

    private Map<String, Object> buildSectionStateJson(NewsletterIssueStateSaveReq.Section section) {
        Map<String, Object> state = new LinkedHashMap<>(normalizeSectionStateMap(section.state()));
        state.remove("collapsed");
        state.remove("showDivider");
        return state;
    }

    private Map<String, Object> buildBlockStateJson(NewsletterIssueStateSaveReq.Block block) {
        Map<String, Object> state = new LinkedHashMap<>(normalizeStateMap(block.state()));
        state.remove("collapsed");
        if (block.plotOnChart() != null) {
            state.put("plotOnChart", block.plotOnChart());
        }
        return state;
    }

    private Map<String, Object> normalizeSectionStateMap(Map<String, Object> source) {
        Map<String, Object> normalized = new LinkedHashMap<>(normalizeStateMap(source));
        normalized.putIfAbsent("removed", Boolean.FALSE);
        return normalized;
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
            } else if (value instanceof Collection<?> collection) {
                normalizedValue = collection.stream()
                    .map(item -> item instanceof Map<?, ?> nestedItem
                        ? normalizeStateMap((Map<String, Object>) nestedItem)
                        : item)
                    .toList();
            }
            normalized.put(normalizedKey, normalizedValue);
        });
        return normalized;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "ISSUE_STATE_JSON_SERIALIZE_FAILED");
        }
    }

    private String toYn(Boolean value) {
        return Boolean.FALSE.equals(value) ? "N" : "Y";
    }
}
