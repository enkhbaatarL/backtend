package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.DesignEditTable;
import jakarta.validation.constraints.NotNull;

/**
 * 단계1 변경사항 검토 요청.
 */
public record ScenarioValidationReq(
    @NotNull DesignEditTable table
) {}
