package com.sk.domain.mi.service.newsletter;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateBlock;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateSection;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateV2;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateBlockFieldRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateBlockRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateSectionRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateV2Repository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateLoader {

    private final NewsletterMasterRepository masterRepository;
    private final NewsletterTemplateV2Repository templateRepository;
    private final NewsletterTemplateSectionRepository sectionRepository;
    private final NewsletterTemplateBlockRepository blockRepository;
    private final NewsletterTemplateBlockFieldRepository blockFieldRepository;

    public TemplateTree loadCurrent(Integer newsletterMasterId) {
        NewsletterMaster master = masterRepository.findById(newsletterMasterId)
            .filter(newsletterMaster -> !newsletterMaster.getIsDeleted())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));

        if (master.getNewsletterTemplateId() == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_TEMPLATE_NOT_FOUND");
        }

        return loadByTemplateId(master.getNewsletterTemplateId());
    }

    public TemplateTree loadByPublishHistory(NewsletterPublishHistory publishHistory) {
        if (publishHistory.getNewsletterTemplateId() == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_TEMPLATE_NOT_FOUND");
        }
        return loadByTemplateId(publishHistory.getNewsletterTemplateId());
    }

    public TemplateTree loadByTemplateId(Long templateId) {
        NewsletterTemplateV2 template = templateRepository.findActiveByTemplateId(templateId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_TEMPLATE_NOT_FOUND"));

        List<NewsletterTemplateSection> sections = sectionRepository
            .findByTemplateV2IdAndDelYnOrderBySctnOrd(template.getNewsletterTemplateV2Id(), "N");

        List<Long> sectionIds = sections.stream()
            .map(NewsletterTemplateSection::getNewsletterTemplateSectionId)
            .toList();

        List<NewsletterTemplateBlock> blocks = sectionIds.isEmpty()
            ? List.of()
            : blockRepository.findBySectionIdInAndDelYnOrderByBlkOrd(sectionIds, "N");

        List<Long> blockIds = blocks.stream()
            .map(NewsletterTemplateBlock::getNewsletterTemplateBlockId)
            .toList();

        return TemplateTree.of(
            template,
            sections,
            blocks,
            blockIds.isEmpty() ? List.of()
                : blockFieldRepository.findByBlockIdInAndDelYnOrderByFldOrd(blockIds, "N")
        );
    }
}
