package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;
import jakarta.validation.constraints.NotNull;

/**
 * 단계1 변경사항 검토 요청.
 * 편집 후 최종 분기항목/시나리오 표를 전달한다 (미채번 행=신규, 빠진 행=삭제).
 */
public record ScenarioValidationReq(
    @NotNull ScenarioDesignTable table
) {}
