package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartV4Req;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.service.nextgen.ScenarioDesignV4Service;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Profile("!dedicated")
@Tag(
    name = "03-2. Next Gen MI Scenario Design V4 (Multi-Event) API",
    description = "멀티 이벤트 시나리오 설계 파이프라인 API입니다. 생성 진행은 Agent SSE 직결 중계(재연결 자동 처리), 설계 화면 데이터는 B.E 계약으로 가공해 제공합니다."
)
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioDesignV4Controller {

    private final ScenarioDesignV4Service scenarioDesignV4Service;

    @ApiOrder(0)
    @Operation(
        summary = "멀티 이벤트 시나리오 그룹 생성 API (SSE, 재연결 겸용)",
        description = """
            선택한 이벤트 1~5개로 시나리오 그룹 생성을 시작하고, **같은 POST 응답에서** 생성 진행 이벤트를 SSE로 계속 수신합니다. <br>
            Agent v4 생성 스트림을 프레임(id/event/data) 그대로 중계하며, 이벤트 상세 계약은 Agent 문서(Scenario Agent API Multi-Events)를 따릅니다. <br>

            <br><br>
            ### 요청 본문
            - `miEventIds`: 설계 대상 이벤트 ID 목록 (1~5개, 서로 다른 양의 정수, **배열 순서가 그룹의 이벤트 표시 순서로 보존**)

            <br><br>
            ### 재연결 자동 처리
            동일 이벤트 조합의 생성 작업이 이미 진행 중이면 오류(409) 대신 **그 작업의 스트림에 자동으로 재연결**됩니다.
            이때 지나간 이벤트가 sequence 0부터 전체 replay된 뒤 live 이벤트가 이어지므로, FE는 sequence 중복만 무시하면 됩니다. <br>
            따라서 연결 끊김·새로고침·브라우저 재시작 어떤 경우든 **같은 조합으로 이 API를 다시 호출하는 것이 복구 방법**입니다.
            (진행 중이던 조합을 모르는 경우: 이벤트 목록/상세의 진행중 그룹 표시에서 진입해 설계 화면 데이터 조회 API의 `baseEvents`로 조합을 복원합니다)

            <br><br>
            ### 전송 형식
            `text/event-stream`. 각 프레임은 `id:`(= sequence), `event:`(이벤트 종류), `data:`(JSON)로 구성되며,
            진행 이벤트가 없는 동안 keep-alive 주석 프레임(`: keep-alive`)이 약 15초 간격으로 전달됩니다(무시). <br>
            ```
            id: 1
            event: accepted
            data: {"operationId":"01KZ...","scenarioGroupId":410,"sequence":1,...,"status":"queued"}
            ```

            <br><br>
            ### 이벤트 종류 & 처리 가이드
            - `accepted`: 접수 — `scenarioGroupId`를 화면 상태에 저장 (로컬 영속 보관 불필요 — 유실 시 같은 조합으로 재호출)
            - `phase_started` / `validated`: 진행 문구 갱신 (`title`, `messageMarkdown` 표시용)
            - `retrying`: **실패가 아님** — 시도 횟수 표시, 실패 화면 전환 금지
            - `section_ready`: `snapshotRevision`이 기존보다 크면 설계 화면 데이터 조회 API 재조회 후 화면 갱신
            - `completed` / `failed` / `interrupted`: terminal — 설계 화면 데이터 조회 API를 최종 1회 조회
              (failed/interrupted면 응답의 `operation.lastError`로 실패 사유 표시)

            <br><br>
            ### 소비 방식
            POST 응답 스트림이므로 브라우저 `EventSource`로는 읽을 수 없고 **fetch streaming**으로 소비합니다.

            <br><br>
            ### 오류 (SSE가 아닌 JSON, BaseResponse 래핑)
            응답 body를 읽기 전에 HTTP status와 `Content-Type`을 먼저 확인하십시오. <br>
            오류 시 Agent 오류 body의 `data` 원문이 `BaseResponse.data`로 그대로 전달됩니다 —
            FE는 `data.error_name`으로 분기하고, `data`의 기존 리소스 ID를 재사용합니다.
            ```
            HTTP 400
            {"status": false, "code": "...", "message": "동일한 Event 조합의 Scenario Group이 이미 존재합니다...",
             "data": {"scenarioGroupId": 410, "error_code": -1006,
                      "error_name": "SCENARIO_GROUP_ALREADY_CREATED"}}
            ```
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream). 동일 조합 작업 진행 중이면 해당 작업 스트림으로 자동 재연결(0부터 replay)")
    @ApiResponse(responseCode = "400", description = "요청 검증 실패(개수/중복/양수), 또는 동일 조합 그룹이 확정 전 존재 (data.error_name=SCENARIO_GROUP_ALREADY_CREATED — scenarioGroupId로 상세 이동)")
    @ApiResponse(responseCode = "403", description = "동일 조합 작업/그룹이 타 사용자 소유 (data.error_name=SCENARIO_GROUP_OWNERSHIP_CONFLICT)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter createScenarioGroupStream(
        @Valid @RequestBody ScenarioDesignStartV4Req request
    ) {
        return scenarioDesignV4Service.startDesignStream(request);
    }

    @ApiOrder(1)
    @Operation(
        summary = "설계 화면 데이터 조회 API (단일 조회 지점)",
        description = """
            시나리오 설계 화면 전체 데이터를 반환하는 **단일 조회 API**입니다. Agent presentation 응답을 B.E 계약으로 가공하고,
            설계 세션 상태(`designSession`)와 검토 편집본 오버레이까지 합성하므로 별도의 세션 상태 조회 API가 필요 없습니다. <br>
            시나리오 표는 기존 v1 설계 표 구조(`table.dimensionColumns` + `table.scenarios`)를 그대로 재사용합니다. <br>

            <br><br>
            ### 호출 시점
            - 생성 중: `section_ready` 수신 시 `snapshotRevision`이 증가했을 때, terminal 이벤트 수신 후 최종 1회
            - 편집 중: 검토/Signpost 콜백 신호(구독 SSE) 수신 후 재조회 — **검토 통과 편집본이 `table`에 오버레이**되어 내려옵니다

            <br><br>
            ### 설계 세션 상태 (`designSession`, B.E 합성)
            - `status`/`stage`(+라벨): 그룹 상태와 설계 단계 — 검토/Signpost/확정 버튼 제어 기준
            - `occupied`/`occupantUserId`: 점유 정보
            - `validation`: 검토 결과 (`isValid`/`reason`) — 검토 미수행이면 null
            - `editOverlayApplied`: true면 `table`이 검토 통과 편집본(DB 미반영, Redis 캐시) 오버레이 상태.
              이때 셀의 `valueId`와 근거는 `dimensionValueOptions`에서 label 역조인으로 복원되며,
              자유입력 등 옵션에 없는 값은 label만 유지됩니다. false면 Agent 영속 상태 기준.

            <br><br>
            ### Signpost 이후 단계 합성 (B.E)
            - `groupMeta`: 그룹명/요약/설명 — Signpost 산출 시 Agent가 채우며, 확정 화면의 편집 초기값입니다
            - `table.scenarios[].signposts[]`: 행별 Signpost 목록 (stage가 signpost_created/completed일 때 DB 합성) —
              확정 요청의 signposts 재투영에 이 데이터를 사용합니다

            <br><br>
            ### 화면 개념 - 응답 필드 매핑
            - **이벤트 리스트** (화면 상단): `baseEvents[]` — 그룹을 구성하는 이벤트 (표시 순서 보존, B.E DB 합성)
            - **분기항목** (표 헤더): `table.dimensionColumns[]` — `key`/`label` + (i) 아이콘용 `description`/`rationale`
            - **분기값** (헤더별 선택 가능한 값 전체): `dimensionValueOptions{key}` — 드롭다운 데이터 (`valueId`/`label`/`rationale`/`pressureContribution`)
            - **분기기준** (시나리오 행별 현재 값): `table.scenarios[].dimensions[]` — `value`(표시 label)와
              선택 상태 매칭용 `valueId`, 셀 (i) 아이콘용 `rationale`/`pressureContribution`
            - **배지**: `scenarios[].scenarioRole`(floor/ceiling/mixed) + `pressureBand`(5단계) — 라벨 필드 동봉, v1 legacy는 null
            - **근거 마크다운**: `evidence.eventAnalysisMarkdown`(Event 분석), `evidence.roleSelectionMarkdown`(대표 시나리오 선정 근거) — sanitize 후 렌더
            - **진행/실패 상태**: `operation` — `status`(코드) + `statusLabel`(표시 라벨, 미지 어휘면 null이므로 코드 폴백).
              terminal 여부는 SSE로 알 수 있으므로 이 API는 폴링 용도가 아니라 표시 용도입니다.
              failed/interrupted면 `operation.lastError`(B.E DB 합성)로 실패 사유를 표시합니다.

            <br><br>
            ### 편집 요청 왕복 규칙
            검토/Signpost 요청 시에는 headers(key/label/displayOrder)와 rows(scenarioId/displayOrder/title/cells)만 보내고
            **cells에는 `dimensions[].value`(표시 label)를 그대로 재전송**합니다. `valueId`/`rationale` 등 근거 필드는 전송하지 않습니다.
            드롭다운에서 다른 분기값을 선택한 경우 `dimensionValueOptions`의 해당 항목 `label`을 cells 값으로 사용합니다.

            <br><br>
            ### 참고
            - 생성 진행 중에는 준비된 섹션만 채워집니다 — 표가 아직 없으면 `table: null`, 마크다운 섹션은 status로 구분합니다.
            - legacy(v1) 그룹도 렌더되며 `legacy: true`와 `warnings`로 근거 제한을 안내합니다 (operation은 null).
            - `operationId`를 주면 해당 작업 기준의 중간 스냅샷을 조회합니다 (생략 시 최신).
            - Agent 오류(404 등)는 HTTP status 그대로 BaseResponse로 래핑되어 전달됩니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/presentation")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignPresentationRes> getPresentation(
        @Parameter(description = "시나리오 그룹 ID", example = "410") @PathVariable Long scenarioGroupId,
        @Parameter(description = "operation ID (생성 진행 중 중간 스냅샷 조회용, 생략 가능)") @RequestParam(required = false) String operationId
    ) {
        return BaseResponseFactory.create(scenarioDesignV4Service.getPresentation(scenarioGroupId, operationId));
    }

}
