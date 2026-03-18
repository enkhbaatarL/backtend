package com.sk.domain.mi.dto.request.newsletter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsletterIssueStateSaveReq(
    List<Section> sections
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(
        Long issueSectionStateId,
        Long templateSectionId,
        Integer order,
        Boolean visibility,
        String title,
        Integer targetCompanyNo,
        Map<String, Object> state,
        List<Block> blocks
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Block(
        Long issueBlockStateId,
        Long templateBlockId,
        Integer order,
        Boolean visibility,
        String title,
        Integer targetCompanyNo,
        Boolean plotOnChart,
        Map<String, Object> state
    ) {
    }
}
