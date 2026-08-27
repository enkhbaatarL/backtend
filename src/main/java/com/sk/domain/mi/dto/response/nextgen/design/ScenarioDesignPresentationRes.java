package com.sk.domain.mi.dto.response.nextgen.design;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.enums.nextgen.DesignOperationPhase;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationType;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 그룹 설계 화면 데이터
 */
public record ScenarioDesignPresentationRes(

    @Schema(description = "시나리오 그룹 ID", example = "410")
    Long scenarioGroupId,

    @Schema(description = "선택된 이벤트 묶음")
    List<EventBundle> eventBundles,

    @Schema(description = "근거 (Event 관계 분석)")
    DesignEvidence evidence,

    @Schema(description = "시나리오 그룹 생성 상태")
    ScenarioGroupCreationState creationState,

    @Schema(description = "시나리오 그룹 정제 상태(validation/Signpost/confirmation) 상태")
    ScenarioGroupRefinementState refinementState,

    @Schema(description = "시나리오 설계 표")
    DesignTable table,

    @Schema(description = "분기항목별 선택 가능한 분기값 목록")
    Map<String, List<DesignDimensionValueOption>> dimensionValueOptions

) {

    public static ScenarioDesignPresentationRes of(Long scenarioGroupId, List<EventBundle> eventBundles,
                                                   DesignEvidence evidence, ScenarioGroupCreationState creationState,
                                                   ScenarioGroupRefinementState refinementState, DesignTable table,
                                                   Map<String, List<DesignDimensionValueOption>> dimensionValueOptions) {
        return new ScenarioDesignPresentationRes(scenarioGroupId, eventBundles, evidence,
            creationState, refinementState, table, dimensionValueOptions);
    }

    /**
     * 그룹을 구성하는 이벤트 번들
     */
    public record EventBundle(

        @Schema(description = "이벤트 ID", example = "1132")
        Long eventId,

        @Schema(description = "이벤트명", example = "[유럽 가스저장] EU 저장률 43.10%와 NWE 할인 확대")
        String eventName,

        @Schema(description = "이벤트 유형 Lv1", example = "exogenous_shock")
        String eventTypeLv1,

        @Schema(description = "이벤트 유형 Lv1 라벨", example = "외생충격")
        String eventTypeLv1Label,

        @Schema(description = "시나리오 제안 이벤트 여부", example = "true")
        Boolean isScenarioSuggested,

        @Schema(description = "최신 연관 레코드 최초 생성일시 (KST, tmm_mi_mievent_m.latest_record_first_created, 응답 시 KST 변환", example = "2026-05-12 15:08:34")
        LocalDateTime latestRecordFirstCreated

    ) {}

    /**
     * 근거 마크다운
     */
    public record DesignEvidence(

        @Schema(description = "Event 연관관계 분석 근거. 생성 전이면 null", nullable = true)
        String eventAnalysisMarkdown,

        @Schema(description = "대표 시나리오 선정 근거. 생성 전이면 null", nullable = true)
        String roleSelectionMarkdown

    ) {}

    /**
     * 그룹 생성 상태 (첫 번째 SSE의 작업 상태)
     */
    public record ScenarioGroupCreationState(

        @Schema(description = "작업 상태 (queued/running/succeeded/failed/interrupted)", example = "succeeded", nullable = true)
        DesignOperationStatus operationStatus,

        @Schema(description = "작업 상태 라벨", example = "성공", nullable = true)
        String operationStatusLabel,

        @Schema(description = "현재 처리 단계 (event_analysis/dimension_design/scenario_synthesis/persistence)", example = "persistence", nullable = true)
        DesignOperationPhase currentPhase,

        @Schema(description = "현재 처리 단계 라벨", example = "결과 저장", nullable = true)
        String currentPhaseLabel,

        @Schema(description = "표시 스냅샷 revision", example = "5", nullable = true)
        Integer snapshotRevision,

        @Schema(description = "실패 사유 원문 (failed/interrupted일 때, B.E DB 합성). 없으면 null", nullable = true)
        JsonNode lastError

    ) {

        public static ScenarioGroupCreationState of(DesignOperationStatus operationStatus, DesignOperationPhase currentPhase,
                                                    Integer snapshotRevision, JsonNode lastError) {
            return new ScenarioGroupCreationState(
                operationStatus,
                operationStatus == null ? null : operationStatus.getLabel(),
                currentPhase,
                currentPhase == null ? null : currentPhase.getLabel(),
                snapshotRevision,
                lastError
            );
        }

    }

    /**
     * 정련 상태 (두 번째 SSE의 상태, B.E 합성) — 생성된 초안을 검토/Signpost/확정으로 다듬는 2차 작업.
     * 단계 전이와 검토 결과는 Agent가 아니라 B.E(그룹 엔티티 + Redis 검토 캐시)가 원천이라 여기서 함께 내린다.
     */
    public record ScenarioGroupRefinementState(

        @Schema(description = "설계 단계 (ready/scenario_created/scenario_validated/signpost_created/completed)", example = "scenario_validated")
        ScenarioGroupStage stage,

        @Schema(description = "설계 단계 라벨", example = "시나리오 검토완료")
        String stageLabel,

        @Schema(description = "점유 여부", example = "true")
        boolean occupied,

        @Schema(description = "점유자가 로그인 사용자인지 (편집 가능/읽기 전용 분기 기준)", example = "true")
        boolean mine,

        @Schema(description = "점유자 사용자 ID", example = "1000078", nullable = true)
        Long occupantUserId,

        @Schema(description = "검토 결과 (isValid/reason). 값이 있으면 table은 해당 검토 편집본이며, 검토 미수행·만료·초기화면 null(table은 마지막 영속 상태)", nullable = true)
        DesignSessionValidation validation,

        @Schema(description = "Agent refine 작업 실행 중 여부. true면 요청 버튼 대신 스피너를 표시하고 SSE 완료 신호 수신 후 재조회한다", example = "true")
        boolean taskRunning,

        @Schema(description = "실행 중인 refine 작업 종류 (validation:검토 / signpost:Signpost 생성).", example = "signpost", nullable = true)
        DesignOperationType runningTaskType,

        @Schema(description = "실행 중인 refine 작업 라벨 (검토/Signpost 생성)", example = "Signpost 생성", nullable = true)
        String runningTaskTypeLabel,

        @Schema(description = "시나리오 그룹 메타 (그룹명/요약/설명 - 생성 시 Agent가 채우고 확정 화면의 편집 초기값으로 사용)", nullable = true)
        ScenarioGroupMeta scenarioGroupMeta

    ) {

        public static ScenarioGroupRefinementState of(ScenarioGroupStage stage,
                                                      boolean occupied, boolean mine, Long occupantUserId,
                                                      DesignSessionValidation validation,
                                                      DesignOperationType runningTaskType,
                                                      ScenarioGroupMeta scenarioGroupMeta) {
            return new ScenarioGroupRefinementState(
                stage,
                stage == null ? null : stage.getLabel(),
                occupied,
                mine,
                occupantUserId,
                validation,
                runningTaskType != null,
                runningTaskType,
                runningTaskType == null ? null : runningTaskType.getLabel(),
                scenarioGroupMeta
            );
        }

    }

    /** 검토 결과 (세션 상태 조회 API의 Validation과 동일 계약). */
    public record DesignSessionValidation(

        @Schema(description = "정합성 통과 여부", example = "true")
        Boolean isValid,

        @Schema(description = "검토 사유/코멘트", nullable = true)
        String reason

    ) {}

    /** 시나리오 그룹 메타 (생성 시 Agent가 채우는 그룹명/요약/설명 - 확정 화면 편집 초기값). */
    public record ScenarioGroupMeta(

        @Schema(description = "시나리오 그룹명", example = "JKM 대표 시나리오 합성 초안")
        String title,

        @Schema(description = "시나리오 그룹 요약", nullable = true)
        String summary,

        @Schema(description = "시나리오 그룹 설명", nullable = true)
        String description

    ) {}

    /**
     * 분기항목의 선택 가능한 분기값 1건 (드롭다운 항목).
     */
    public record DesignDimensionValueOption(

        @Schema(description = "분기값 ID - 행 dimensions[].valueId와 매칭해 선택 상태 표시", example = "d1_v1")
        String valueId,

        @Schema(description = "분기값 표시 label - 선택 시 셀 value로 사용", example = "구매계획 유지 국면")
        String label,

        @Schema(description = "분기값 근거", nullable = true)
        String rationale

    ) {}

}
