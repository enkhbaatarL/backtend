package com.sk.domain.mi.dto.request.newsletter;

import java.util.List;
import java.util.Map;

public record NewsletterIssueStateSaveReq(
    List<Section> sections
) {

    public record Section(
        Long issueSectionStateId,
        Long templateSectionId,
        Integer order,
        Boolean visibility,
        String title,
        Integer targetCompanyNo,
        Boolean collapsed,
        Map<String, Object> state,
        List<Block> blocks
    ) {
    }

    public record Block(
        Long issueBlockStateId,
        Long templateBlockId,
        Integer order,
        Boolean visibility,
        String title,
        Integer targetCompanyNo,
        Boolean collapsed,
        Boolean plotOnChart,
        Map<String, Object> state
    ) {
    }
}
