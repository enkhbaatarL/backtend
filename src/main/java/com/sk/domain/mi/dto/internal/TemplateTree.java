package com.sk.domain.mi.dto.internal;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateBlock;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateBlockField;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateSection;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateV2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record TemplateTree(
    TemplateNode template,
    List<SectionNode> sections
) {

    public static TemplateTree of(
        NewsletterTemplateV2 template,
        List<NewsletterTemplateSection> sections,
        List<NewsletterTemplateBlock> blocks,
        List<NewsletterTemplateBlockField> fields
    ) {
        Map<Long, List<FieldNode>> fieldsByBlock = fields.stream()
            .map(FieldNode::from)
            .collect(Collectors.groupingBy(FieldNode::blockId));

        Map<Long, List<BlockNode>> blocksBySection = blocks.stream()
            .map(block -> BlockNode.from(
                block,
                fieldsByBlock.getOrDefault(block.getNewsletterTemplateBlockId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(FieldNode::fieldOrder))
                    .toList()
            ))
            .collect(Collectors.groupingBy(BlockNode::sectionId));

        List<SectionNode> sectionNodes = sections.stream()
            .map(section -> SectionNode.from(
                section,
                blocksBySection.getOrDefault(section.getNewsletterTemplateSectionId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(BlockNode::blockOrder))
                    .toList()
            ))
            .sorted(Comparator.comparingInt(SectionNode::sectionOrder))
            .toList();

        return new TemplateTree(TemplateNode.from(template), sectionNodes);
    }

    public List<BlockNode> allBlocks() {
        List<BlockNode> allBlocks = new ArrayList<>();
        sections.forEach(section -> allBlocks.addAll(section.blocks()));
        return allBlocks;
    }

    public record TemplateNode(
        Long id,
        String name,
        Integer masterId,
        Integer schemaVersion
    ) {
        private static TemplateNode from(NewsletterTemplateV2 template) {
            return new TemplateNode(
                template.getNewsletterTemplateV2Id(),
                template.getTemplateName(),
                template.getNewsletterMasterId(),
                template.getSchemaVersion()
            );
        }
    }

    public record SectionNode(
        Long id,
        String sectionCode,
        String sectionTypeCode,
        String sectionName,
        Integer sectionOrder,
        String visibilityYn,
        Integer targetCompanyNo,
        List<BlockNode> blocks
    ) {
        private static SectionNode from(NewsletterTemplateSection section, List<BlockNode> blocks) {
            return new SectionNode(
                section.getNewsletterTemplateSectionId(),
                section.getSectionCode(),
                section.getSectionTypeCode(),
                section.getSectionName(),
                section.getSectionOrder(),
                section.getVisibilityYn(),
                section.getTargetCompanyNo(),
                blocks
            );
        }
    }

    public record BlockNode(
        Long id,
        Long sectionId,
        String blockCode,
        String blockTypeCode,
        String blockFormatCode,
        String blockName,
        Integer blockOrder,
        String visibilityYn,
        String plotOnChartYn,
        Integer targetCompanyNo,
        String dataSourceType,
        String dataSourceRef,
        List<FieldNode> fields
    ) {
        private static BlockNode from(NewsletterTemplateBlock block, List<FieldNode> fields) {
            return new BlockNode(
                block.getNewsletterTemplateBlockId(),
                block.getSectionId(),
                block.getBlockCode(),
                block.getBlockTypeCode(),
                block.getBlockFormatCode(),
                block.getBlockName(),
                block.getBlockOrder(),
                block.getVisibilityYn(),
                block.getPlotOnChartYn(),
                block.getTargetCompanyNo(),
                block.getDataSourceTypeCode(),
                block.getDataSourceRef(),
                fields
            );
        }
    }

    public record FieldNode(
        Long id,
        Long blockId,
        String fieldKey,
        String fieldName,
        Integer fieldOrder,
        String colMapping,
        String isEditableYn,
        String isRequiredYn,
        String fieldMeta
    ) {
        private static FieldNode from(NewsletterTemplateBlockField field) {
            return new FieldNode(
                field.getNewsletterTemplateBlockFieldId(),
                field.getBlockId(),
                field.getFieldKey(),
                field.getFieldName(),
                field.getFieldOrder(),
                field.getColMapping(),
                field.getIsEditableYn(),
                field.getIsRequiredYn(),
                field.getFieldMeta()
            );
        }
    }
}
