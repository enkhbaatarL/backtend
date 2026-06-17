package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.DesignConfirmTable;
import jakarta.validation.constraints.NotNull;

/**
 * 최종 확정 요청 (단계2 편집 일괄 적용 + 확정).
 * 그룹 편집(이름/요약/설명) + full 편집 표({@link DesignConfirmTable})를 전달한다
 * (narrative/확률/baseCase/Signpost 편집 포함, Signpost는 사용여부 토글 포함). B.E가 Agent 계약 형태로 변환해 확정을 프록시한다.
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
