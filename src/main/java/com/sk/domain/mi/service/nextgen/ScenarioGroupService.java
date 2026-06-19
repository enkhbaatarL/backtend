package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;

public interface ScenarioGroupService {

    /** 그룹 모드 — 시나리오 그룹 목록 (펼침 시 시나리오 표 포함) */
    ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request);

    /** 개별 모드 — 모든 그룹의 시나리오를 평탄화한 평면 목록 */
    ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request);

    /** 그룹 펼침 그래프 데이터 (단일 인덱스 × 단일 기간단위 — Base + 시나리오 시리즈) */
    ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit);

    /** 시나리오 그룹 상세 (요약 + 가정·실행가능성 표) */
    ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId);

    /** 개별 시나리오 상세 (내러티브 + 발생확률 + 주요 가정 + Signpost) */
    ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId);

    /** 시나리오 판정 상태 변경 (대기/실현/폐기) */
    ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioDisplayStatus status);

    /** 시나리오 그룹 삭제 (그룹 + 하위 시나리오 soft delete) */
    void deleteScenarioGroup(Long scenarioGroupId);

}
