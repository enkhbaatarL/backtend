package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignGroup;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;

/**
 * [Agent 콜백] 시나리오 검토 완료 본문 (Agent → B.E).
 * <p>
 * 검토는 DB 적재 없이(write-then-callback 대상 아님) reconcile 결과 table을 그대로 전달한다.
 * table.rows[].cells에는 Agent 자동생성값(신규 분기항목 등)이 채워져 오며 DB에는 저장하지 않는다.
 * <ul>
 *   <li>{@code result}: Agent 검토 작업 성공 여부 (작업 실패면 FAILURE)</li>
 *   <li>{@code isValid}: 정합성 통과 여부 — true면 stage scenario_validated, false면 scenario_created 유지 + reason</li>
 * </ul>
 */
public record ScenarioValidationCallbackReq(
    AgentCallbackResult result,
    Boolean isValid,
    String reason,
    ScenarioDesignGroup scenarioGroup,
    ScenarioDesignTable table
) {}
