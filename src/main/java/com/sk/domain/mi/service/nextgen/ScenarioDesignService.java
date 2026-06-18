package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;

/**
 * 시나리오 설계 파이프라인 (FE-facing). 시작 → 검토 → Signpost 생성 → 확정.
 * 실 구현은 Agent 프록시, mock 구현은 인메모리 세션 시뮬레이션이며 {@link com.sk.domain.mi.config.NextGenMockProperties}로 라우팅된다.
 */
public interface ScenarioDesignService {

    /** 설계 시작 (점유 + Agent 시나리오 생성 요청). */
    ScenarioDesignStageRes startDesign(ScenarioDesignStartReq request);

    /** 설계 세션 상태 조회 (검토 캐시 오버레이 / DB 초기값). */
    ScenarioDesignStateRes getDesign(Long scenarioGroupId);

    /** 분기항목/분기기준 템플릿 조회. */
    ScenarioDimensionTemplateRes getDimensionTemplates(Long scenarioGroupId);

    /** 변경사항 검토 요청 (DB 미반영). */
    ScenarioDesignStageRes validateDesign(Long scenarioGroupId, ScenarioValidationReq request);

    /** 단계1 완료 / 단계2 진입 (영속화 + Signpost 산출). */
    ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request);

    /** 최종 확정 (스냅샷 저장 + 검토 캐시 정리). */
    ScenarioDesignStageRes confirmDesign(Long scenarioGroupId, ScenarioConfirmationReq request);

    /** 전체 초기화 (검토 캐시 삭제 후 DB 초기값 반환). */
    ScenarioDesignStateRes reset(Long scenarioGroupId);

}
