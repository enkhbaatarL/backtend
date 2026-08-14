package com.sk.domain.mi.dto.response.nextgen.design;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 설계 화면 데이터 (v4 멀티 이벤트) — Agent presentation 응답을 B.E 계약으로 가공한 결과.
 *
 * Agent의 sections 트리를 화면 단위(baseEvents, evidence, table, dimensionValueOptions)로 펴서 내리고,
 * table은 기존 설계 표 계약(DesignTable)을 그대로 재사용한다 — FE는 v1 설계 표 렌더러와 편집 요청
 * 조립 로직을 공유할 수 있다. dimensions[].value는 표시 label이므로 검토/Signpost 요청의 cells에
 * 그대로 재전송하면 기존 계약이 충족된다.
 *
 * baseEvents와 operation.lastError는 Agent 응답이 아니라 B.E가 DB(그룹-이벤트 매핑, operation 테이블)에서
 * 합성한다 — Agent presentation에는 이벤트 메타와 실패 사유 원문이 없다.
 *
 * designSession도 B.E 합성이다 — 설계 세션의 단계(stage)/점유/검토 결과는 B.E가 관리하는 상태라
 * Agent는 모른다. 검토를 통과한 편집본(DB 미반영, Redis 캐시)이 있으면 table을 그 편집본으로
 * 오버레이해 내리므로, 설계 화면은 진입부터 검토 재조회까지 이 API 하나만 사용하면 된다
 * (기존 세션 상태 조회 API 대체 — 2026-08-14 단일화 결정).
 */
public record ScenarioDesignPresentationRes(

    @Schema(description = "시나리오 그룹 ID", example = "410")
    Long scenarioGroupId,

    @Schema(description = "생성 엔진 버전 (v4=멀티 이벤트, v1=legacy)", example = "v4")
    String engineVersion,

    @Schema(description = "legacy(v1) 그룹 여부 - true면 근거 필드가 제한(limited)되고 operation이 null", example = "false")
    boolean legacy,

    @Schema(description = "표시 경고 문구 (주로 legacy 복원 시)", example = "[\"기존 v1 데이터에는 상세 생성 근거가 저장되지 않았습니다.\"]")
    List<String> warnings,

    @Schema(description = "생성 작업 상태 (legacy 그룹이면 null)", nullable = true)
    DesignOperationSummary operation,

    @Schema(description = "설계 세션 상태 (stage/점유/검토 결과, B.E 합성). 그룹이 B.E DB에 없으면 null", nullable = true)
    DesignSessionState designSession,

    @Schema(description = "그룹 메타 (그룹명/요약/설명 - Signpost 이후 단계의 편집 초기값, B.E 합성). 그룹이 B.E DB에 없으면 null", nullable = true)
    DesignGroupMeta groupMeta,

    @Schema(description = "그룹을 구성하는 이벤트 목록 (표시 순서 보존, B.E DB 합성)")
    List<DesignBaseEvent> baseEvents,

    @Schema(description = "근거 마크다운 섹션 (Event 분석, 대표 시나리오 선정 근거)")
    DesignEvidence evidence,

    @Schema(description = "시나리오 설계 표 (기존 설계 표 계약 재사용). 아직 준비되지 않았으면 null", nullable = true)
    DesignTable table,

    @Schema(description = "분기항목별 선택 가능한 분기값 목록 - key는 table.dimensionColumns[].key와 매칭 (드롭다운 데이터)")
    Map<String, List<DesignDimensionValueOption>> dimensionValueOptions

) {

    public static ScenarioDesignPresentationRes of(Long scenarioGroupId, String engineVersion, boolean legacy,
                                                   List<String> warnings, DesignOperationSummary operation,
                                                   DesignSessionState designSession, DesignGroupMeta groupMeta,
                                                   List<DesignBaseEvent> baseEvents, DesignEvidence evidence,
                                                   DesignTable table,
                                                   Map<String, List<DesignDimensionValueOption>> dimensionValueOptions) {
        return new ScenarioDesignPresentationRes(scenarioGroupId, engineVersion, legacy, warnings, operation,
            designSession, groupMeta, baseEvents, evidence, table, dimensionValueOptions);
    }

    /** 그룹 메타 (Signpost 산출 시 Agent가 채우는 그룹명/요약/설명 - 확정 화면 편집 초기값). */
    public record DesignGroupMeta(

        @Schema(description = "시나리오 그룹명", example = "JKM 대표 시나리오 합성 초안")
        String title,

        @Schema(description = "시나리오 그룹 요약", nullable = true)
        String summary,

        @Schema(description = "시나리오 그룹 설명", nullable = true)
        String description

    ) {}

    /**
     * 설계 세션 상태 (B.E 합성). 편집 루프의 단계 전이와 검토 결과는 Agent가 아니라
     * B.E(그룹 엔티티 + Redis 검토 캐시)가 원천이라 여기서 함께 내린다.
     */
    public record DesignSessionState(

        @Schema(description = "그룹 상태 (creating/created/realized)", example = "creating")
        ScenarioGroupStatus status,

        @Schema(description = "그룹 상태 라벨", example = "생성중")
        String statusLabel,

        @Schema(description = "설계 단계 (ready/scenario_created/scenario_validated/signpost_created/completed)", example = "scenario_validated")
        ScenarioGroupStage stage,

        @Schema(description = "설계 단계 라벨", example = "시나리오 검토완료")
        String stageLabel,

        @Schema(description = "점유 여부", example = "true")
        boolean occupied,

        @Schema(description = "점유자 사용자 ID", example = "1000078", nullable = true)
        Long occupantUserId,

        @Schema(description = "검토 결과 (검토 미수행이면 null)", nullable = true)
        DesignSessionValidation validation,

        @Schema(description = "table이 검토 통과 편집본(Redis 캐시, DB 미반영) 오버레이인지 - false면 Agent 영속 상태", example = "true")
        boolean editOverlayApplied

    ) {

        public static DesignSessionState of(ScenarioGroupStatus status, ScenarioGroupStage stage,
                                            boolean occupied, Long occupantUserId,
                                            DesignSessionValidation validation, boolean editOverlayApplied) {
            return new DesignSessionState(
                status,
                status == null ? null : status.getLabel(),
                stage,
                stage == null ? null : stage.getLabel(),
                occupied,
                occupantUserId,
                validation,
                editOverlayApplied
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

    /**
     * 생성 작업 상태 요약. status/currentPhase/latestSequence/snapshotRevision은 Agent 응답,
     * lastError는 B.E가 operation 테이블에서 합성한다 (failed/interrupted 실패 사유 표시용).
     */
    public record DesignOperationSummary(

        @Schema(description = "operation ID (ULID)", example = "01KZQJK500D0PE7XVQHXJFMHDY")
        String operationId,

        @Schema(description = "작업 상태 (queued/running/succeeded/failed/interrupted)", example = "succeeded")
        String status,

        @Schema(description = "작업 상태 라벨 (Agent가 새 어휘를 추가한 경우 null - 이때는 코드를 그대로 표시)", example = "성공", nullable = true)
        String statusLabel,

        @Schema(description = "현재 처리 단계", example = "persistence", nullable = true)
        String currentPhase,

        @Schema(description = "최신 이벤트 sequence", example = "15", nullable = true)
        Integer latestSequence,

        @Schema(description = "표시 스냅샷 revision - FE는 마지막 반영값보다 클 때만 재조회", example = "5", nullable = true)
        Integer snapshotRevision,

        @Schema(description = "실패 사유 원문 (failed/interrupted일 때, B.E DB 합성). 없으면 null", nullable = true)
        JsonNode lastError

    ) {}

    /** 그룹을 구성하는 이벤트 (설계 화면 상단 이벤트 리스트). */
    public record DesignBaseEvent(

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

        @Schema(description = "이벤트 업데이트 일시")
        LocalDateTime updatedAt

    ) {}

    /** 근거 마크다운 섹션 묶음. status가 ready가 아니면 markdown은 null일 수 있다. */
    public record DesignEvidence(

        @Schema(description = "Event 분석 섹션 상태 (pending/ready)", example = "ready", nullable = true)
        String eventAnalysisStatus,

        @Schema(description = "Event 분석 마크다운 (sanitize 후 렌더)", nullable = true)
        String eventAnalysisMarkdown,

        @Schema(description = "대표 시나리오(Floor/Ceiling/Mixed) 선정 근거 섹션 상태", example = "ready", nullable = true)
        String roleSelectionStatus,

        @Schema(description = "대표 시나리오 선정 근거 마크다운 (sanitize 후 렌더)", nullable = true)
        String roleSelectionMarkdown

    ) {}

    /** 분기항목의 선택 가능한 분기값 1건 (드롭다운 항목). */
    public record DesignDimensionValueOption(

        @Schema(description = "분기값 ID - 행 dimensions[].valueId와 매칭해 선택 상태 표시", example = "d1_v1")
        String valueId,

        @Schema(description = "분기값 표시 label - 선택 시 셀 value로 사용", example = "구매계획 유지 국면")
        String label,

        @Schema(description = "분기값 근거 (JKM 영향)", nullable = true)
        String rationale,

        @Schema(description = "JKM 압력 기여 (bearish/balanced/bullish)", example = "balanced", nullable = true)
        String pressureContribution

    ) {}

}
