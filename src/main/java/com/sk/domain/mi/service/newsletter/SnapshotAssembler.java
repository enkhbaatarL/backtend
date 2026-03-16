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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotAssembler {

    private final ObjectMapper objectMapper;

    public String assemble(TemplateTree template, Map<String, ResolvedBlockData> resolvedData, NewsletterPublishHistory pubHist) {
        try {
            return assembleNode(template, resolvedData, pubHist).toString();
        } catch (Exception e) {
            throw new IllegalStateException("SNAPSHOT_ASSEMBLY_FAILED", e);
        }
    }

    public JsonNode assembleNode(TemplateTree template, Map<String, ResolvedBlockData> resolvedData, NewsletterPublishHistory pubHist) {
        SnapshotMeta meta = SnapshotMeta.builder()
            .title(pubHist.getNewsletterTitle())
            .emailSubject(pubHist.getEmailSubject())
            .aiSummary(pubHist.getAiSummary())
            .subInfoText(resolveSubInfoText(template, resolvedData))
            .build();

        List<SnapshotSection> sections = new ArrayList<>();
        for (TemplateTree.SectionNode sectionNode : template.sections()) {
            List<SnapshotBlock> blocks = new ArrayList<>();
            for (TemplateTree.BlockNode blockNode : sectionNode.blocks()) {
                blocks.add(SnapshotBlock.builder()
                    .id("blk-" + blockNode.blockCode())
                    .type(blockNode.blockTypeCode())
                    .format(blockNode.blockFormatCode())
                    .order(blockNode.blockOrder())
                    .visibility("Y".equals(blockNode.visibilityYn()))
                    .isCollapsed(false)
                    .title(blockNode.blockName())
                    .plotOnChart("Y".equals(blockNode.plotOnChartYn()))
                    .target(resolveTarget(blockNode.targetCompanyNo()))
                    .editableFields(blockNode.fields().stream().filter(field -> "Y".equals(field.isEditableYn())).map(TemplateTree.FieldNode::fieldKey).toList())
                    .content(resolvedData.getOrDefault(blockNode.blockCode(), ResolvedBlockData.of(Map.of())).toContentMap())
                    .build());
            }
            sections.add(SnapshotSection.builder()
                .id("sec-" + sectionNode.sectionCode())
                .type(sectionNode.sectionTypeCode())
                .title(sectionNode.sectionName())
                .order(sectionNode.sectionOrder())
                .visibility("Y".equals(sectionNode.visibilityYn()))
                .target(resolveTarget(sectionNode.targetCompanyNo()))
                .isCollapsed(false)
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
}
