package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.DesignEditTable;
import jakarta.validation.constraints.NotNull;

/**
 * 단계1 완료 / 단계2 진입 요청 (Signpost 생성).
 * 검토와 동일한 편집 표({@link DesignEditTable})를 전달한다 (검토 후 추가된 신규 시나리오는 scenarioId=null).
 * 이 시점에 DB 영속화되며, B.E가 Agent 계약 형태로 변환해 Signpost 산출을 프록시한다.
 */
public record ScenarioSignpostsReq(
    @NotNull DesignEditTable table
) {}
