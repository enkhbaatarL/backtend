package com.sk.domain.mi.dto.request.nextgen.design;

import jakarta.validation.constraints.NotNull;

/**
 * 시나리오 설계 시작 요청.
 */
public record ScenarioDesignStartReq(
    @NotNull Long miEventId
) {}
