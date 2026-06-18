package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.DesignConfirmTable;
import jakarta.validation.constraints.NotNull;

/**
 * 최종 확정 요청 (단계2 편집 일괄 적용 -> 확정).
 */
public record ScenarioConfirmationReq(
    GroupEdit scenarioGroup,
    @NotNull DesignConfirmTable table
) {

    /** 그룹 편집 항목. */
    public record GroupEdit(
        String title,
        String summary,
        String description
    ) {}

}
