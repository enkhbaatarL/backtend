package com.sk.domain.mi.dto.request.nextgen.design;

import jakarta.validation.constraints.NotNull;

/**
 * 시나리오 설계 시작 요청.
 * 요청 사용자(점유자)는 인증 컨텍스트(ctm id)에서 식별한다.
 */
public record ScenarioDesignStartReq(
    @NotNull Long miEventId
) {}
