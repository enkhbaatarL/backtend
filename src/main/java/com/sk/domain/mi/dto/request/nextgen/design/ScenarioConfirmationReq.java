package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;
import jakarta.validation.constraints.NotNull;

/**
 * 최종 확정 요청 (단계2 편집 일괄 적용 + 확정).
 * 그룹 편집(이름/요약/설명) + 시나리오 표(narrative/가능성/Signpost 포함)를 한 번에 전달한다.
 */
public record ScenarioConfirmationReq(
    GroupEdit scenarioGroup,
    @NotNull ScenarioDesignTable table
) {

    /** 그룹 편집 항목. */
    public record GroupEdit(String title, String summary, String description) {}
}
