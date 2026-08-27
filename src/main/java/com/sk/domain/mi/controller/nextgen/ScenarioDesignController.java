package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.design.*;
import com.sk.domain.mi.dto.response.nextgen.design.AdditionalDimensionsRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStatusRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.ScenarioDesignService;
import com.sk.domain.mi.service.nextgen.ScenarioDesignV4Service;
import com.sk.domain.mi.service.nextgen.sse.registry.ScenarioDesignSseRegistry;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Profile("!dedicated")
@Tag(name = "03-1. Next Gen MI Scenario Design API", description = "Next Gen MI 시나리오 설계 파이프라인 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioDesignController {

    private final ScenarioDesignSseRegistry sseRegistry;
    private final ScenarioDesignService scenarioDesignService;
    private final ScenarioDesignV4Service scenarioDesignV4Service;

    @ApiOrder(1)
    @Operation(
        summary = "이벤트 조합 설계 상태 조회 API",
        description = """
            선택한 이벤트 조합과 일치하는 시나리오 그룹의 설계 상태를 조회합니다. <br>
            이벤트 목록 플로팅바의 CTA 분기에 사용합니다. (이벤트 선택 변경 시마다 호출)

            <br><br>
            ### 요청 파라미터
            - `miEventIds`: 선택한 이벤트 ID 목록 (1~5개)

            <br><br>
            ### 응답 필드
            - `designStatus` / `designStatusLabel`: `designable`(설계가능) / `creating`(설계중) / `created`(설계완료)
            - `scenarioGroupId`: 일치 그룹 ID (`designable`이면 null)
            - `designStep` / `designStepLabel`: 설계중 세부 단계 — `step1`(분기항목 설계) / `step2`(Signpost 설계). 설계중이 아니면 null
            - `occupied` / `occupantUserId` / `occupantUserName`: 점유 여부와 점유 사용자 (설계중일 때만)
            - `mine`: 점유자가 로그인 사용자인지 (설계중일 때만, 그 외 null)

            <br><br>
            ### designStatus별 CTA 분기
            - `designable`: 일치 그룹 없음 — "시나리오 설계" 버튼 노출
            - `creating` + `mine=true`: 로그인 사용자 진행 중 — 단계 배지 표시, 클릭 시 해당 단계 화면으로 이동
            - `creating` + `mine=false`: 다른 사용자가 진행 중 — 잠금 + 단계 배지 표시, 클릭 시 안내 모달
            - `created`: 완료 그룹 존재 — 클릭 시 그룹 상세로 이동
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/design-status")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStatusRes> getDesignStatus(
        @Parameter(description = "선택한 이벤트 ID 목록 (1~5개)", example = "1463,1464") @RequestParam List<Long> miEventIds
    ) {
        return BaseResponseFactory.create(scenarioDesignV4Service.getDesignStatus(miEventIds));
    }

    @ApiOrder(2)
    @Operation(
        summary = "멀티 이벤트 기반 시나리오 그룹 생성 API (SSE)",
        description = """
            최대 5개의 이벤트를 선택하여 시나리오 그룹을 생성합니다. <br>
            생성 중 진행되는 이벤트 메시지를 SSE를 통하여 수신받을 수 있습니다.

            <br><br>
            ### 요청 본문
            - `miEventIds`: 설계 대상 이벤트 ID 목록 (1~5개)
            - `operationId` 오퍼레이션 ID (재연결 시, 사용)
            - `lastSequence`: 마지막 시퀀스 (재연결 시, 사용)

            <br><br>
            ### 재연결 자동 처리
            연결이 끊기면 **동일한 `miEventIds` 조합으로 API를 다시 호출합니다.**
            동일 조합의 진행중 작업이 있으면 B.E가 새 생성 대신 그 작업의 스트림으로 자동으로 재연결합니다. <br>
            이때 **커서 쌍(`operationId`+`lastSequence`)을 함께 보내면, 재연결 대상 작업과 `operationId`가 일치할 때 그 다음 sequence부터 replay** 됩니다.
            (불일치하거나 생략하면 0부터 전체 replay — 중복은 커서로 무시)

            <br><br>
            ### sequence 커서 (FE 관리)
            F.E는 스트림 수신 중 `accepted`의 `operationId`와 마지막으로 처리한 `sequence`를 **쌍으로** 유지하다가, 재연결 재호출 때 그대로 보냅니다.
            SSE 종료 event를 처리하면 커서 쌍을 폐기합니다 (끝난 작업의 커서를 새 작업에 적용하지 않기 위함).

            <br><br>
            ### 전송 형식
            `text/event-stream`. <br>
            **요청의 Accept 헤더는 `text/event-stream, application/json`으로 보냅니다** — 접수 실패 시 오류는
            SSE가 아니라 JSON(BaseResponse)으로 응답되는데, Accept에 JSON이 없으면 콘텐츠 협상에 걸려 오류 본문이 비어
            409 재연결 식별자(scenarioGroupId/operationId)를 받을 수 없습니다. <br>
            각 프레임은 `id:`(= sequence), `event:`(이벤트 종류), `data:`(JSON)로 구성됩니다. <br>
            진행 이벤트가 없는 동안 keep-alive 주석 프레임(`: keep-alive`)이 약 15초 간격으로 전달됩니다.
            (id/event/data가 없는 주석 프레임이므로 연결 생존 신호로만 취급(무시)합니다.)

            <br><br>
            ### data 공통 필드
            모든 이벤트의 data JSON이 공통으로 갖는 필드
            - `operationId`: 작업 식별자 (26자리 ULID)
            - `scenarioGroupId`: 생성된 시나리오 그룹 ID
            - `sequence`: 작업 내 1부터 증가하는 이벤트 순번
            - `eventType`: SSE `event:`와 같은 값
            - `phase`: 현재 생성 단계 — `event_analysis`(Event 분석) → `dimension_design`(분기항목 설계) → `scenario_synthesis`(시나리오 합성) → `persistence`(결과 저장) 순으로 진행
            - `title` / `messageMarkdown`: 사용자 표시용 제목/메시지
            - `snapshotRevision`(number): 화면 스냅샷 revision — `section_ready` 재조회 판단 기준
            - `mlflowTraceId` / `createdAt`: 진단·추적용

            <br><br>
            ### 이벤트 종류 & 처리 가이드
            **(1) `accepted`** — 생성 요청 접수. 항상 첫 이벤트(sequence=1)이며, 그룹/operation/이력이 같은 트랜잭션으로 DB에 저장된 뒤 전달됩니다. 즉 이 이벤트를 받은 이후에는 연결이 끊겨도 서버의 생성 작업은 계속됩니다.
            ```
            id: 1
            event: accepted
            data: {"operationId":"01KZQJK500D0PE7XVQHXJFMHDY","scenarioGroupId":410,"sequence":1,"eventType":"accepted","phase":null,"title":"시나리오 그룹 생성을 접수했습니다","messageMarkdown":"선택한 Event를 바탕으로 시나리오 그룹 생성을 시작합니다.","snapshotRevision":0,"mlflowTraceId":null,"createdAt":"2026-08-11T04:51:47.392548Z","status":"queued","engineVersion":"v4","operationType":"create"}
            ```

            **(2) `phase_started`** — 새 생성 phase 시작. phase와 안내 문구(`title`/`messageMarkdown`)를 갱신합니다.
            ```
            id: 2
            event: phase_started
            data: {"operationId":"01KZQJK500D0PE7XVQHXJFMHDY","scenarioGroupId":410,"sequence":2,"eventType":"phase_started","phase":"event_analysis","title":"Event 분석을 시작했습니다","messageMarkdown":"선택한 Event의 LNG 시장 영향 경로를 분석하고 있습니다.","snapshotRevision":0,"mlflowTraceId":"15743a93-5a6f-44a4-a510-ef1ea51b5b0c","createdAt":"2026-08-11T04:51:47.409650Z"}
            ```

            **(3) `validated`** — 세부 단계 검증 완료. 안내 문구만 갱신합니다.

            **(4) `retrying`** — 검증 기준 미충족으로 현재 단계를 자동 재시도. 안내 문구만 갱신합니다.
            ```
            id: 5
            event: retrying
            data: {"operationId":"01KZQJK500D0PE7XVQHXJFMHDY","scenarioGroupId":410,"sequence":5,"eventType":"retrying","phase":"event_analysis","title":"Event 분석을 다시 수행합니다","messageMarkdown":"검증 기준을 충족하지 못해 Event 분석을 다시 수행합니다.","snapshotRevision":1,"mlflowTraceId":"15743a93-5a6f-44a4-a510-ef1ea51b5b0c","createdAt":"2026-08-11T04:52:24.341159Z","attempt":2,"maxAttempts":3,"referenceIds":[],"validationCode":"LLM_INVALID_RESPONSE_ERROR"}
            ```

            **(5) `section_ready`** — 검증된 화면 섹션 준비 신호. **`snapshotRevision`이 마지막 F.E 반영된 값보다 클 때만** [설계 화면 데이터 조회 API]를 재조회해 화면을 갱신합니다.
            (같은 revision 재수신·replay 중복이면 재조회 생략).
            ```
            id: 4
            event: section_ready
            data: {"operationId":"01KZQJK500D0PE7XVQHXJFMHDY","scenarioGroupId":410,"sequence":4,"eventType":"section_ready","phase":"event_analysis","title":"Event 분석 화면을 갱신할 수 있습니다","messageMarkdown":"검증된 Event 분석 결과가 준비되었습니다.","snapshotRevision":1,"mlflowTraceId":"15743a93-5a6f-44a4-a510-ef1ea51b5b0c","createdAt":"2026-08-11T04:52:11.771814Z"}
            ```

            **(6) `SSE 종료 event`** — 스트림 종료 신호. [설계 화면 데이터 조회 API]를 최종적으로 1회 조회합니다.
            - `completed`: 생성 및 저장 성공함.
            - `failed`: 생성 실패함.
            - `interrupted`: 서버 종료 등으로 중단됨.
            ```
            id: 15
            event: completed
            data: {"operationId":"01KZQJK500D0PE7XVQHXJFMHDY","scenarioGroupId":410,"sequence":15,"eventType":"completed","phase":"persistence","title":"시나리오 그룹 생성이 완료되었습니다","messageMarkdown":"5개 Scenario를 생성했습니다.","snapshotRevision":5,"mlflowTraceId":"15743a93-5a6f-44a4-a510-ef1ea51b5b0c","createdAt":"2026-08-11T04:54:38.088670Z","status":"succeeded"}
            ```

            <br><br>
            ### 이벤트 순서 주의
            한 phase 안에 검증 지점이 여러 개일 수 있어(예: `event_analysis` 안의 Event 개별 분석 + Event 상호작용 분석), 같은 phase에서 `validated`/`section_ready`/`retrying`이 여러 번 발생하는 것이 정상입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream)")
    @ApiResponse(responseCode = "400", description = "요청 검증 실패(개수/중복/양수, B.E 선검증)")
    @ApiResponse(responseCode = "403", description = "동일 조합 작업/그룹이 타 사용자 소유. 오류 수신 시 [이벤트 조합 설계 상태 조회 API]를 재조회해 CTA를 갱신합니다 (JSON 오류는 code/message만 내려감)")
    @ApiResponse(responseCode = "500", description = "Agent 오류 — 409/403 외 Agent의 모든 오류 응답은 B.E-Agent 구간 문제로 취급해 API-004(외부 API 오류 표준 메시지)로 내려갑니다 (원문은 서버 로그)")
    @PostMapping(value = "/creation/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter createScenarioGroupStream(
        @Valid @RequestBody ScenarioDesignStartV4Req request
    ) {
        return scenarioDesignV4Service.startDesignStream(request);
    }

    @ApiOrder(3)
    @Operation(
        summary = "설계 화면 데이터 조회 API",
        description = """
            시나리오 설계 전체 데이터를 반환하는 **단일 조회 API**입니다. <br>

            <br><br>
            ### 호출 시점
            - 생성 중: `section_ready` 수신 시 `snapshotRevision`이 증가했을 때, SSE 종료 event 수신 후 최종 1회
            - 편집 중: 검토/Signpost 콜백 신호([시나리오 검토/Signpost 생성 API (SSE)]) 수신 후 재조회 —
              **검토 통과 편집본이 `table`에 오버레이**되어 내려옵니다

            <br><br>
            ### 정련 상태 (`refinementState`, B.E 합성)
            - `stage`(+라벨): 설계 단계 — 검토/Signpost/확정 버튼 제어 기준
            - `occupied`/`occupantUserId`: 점유 정보
            - `validation`: 검토 결과 (`isValid`/`reason`). **값이 있으면 `table`은 그 검토 편집본**(DB 미반영, 셀 valueId/근거는 서버가 복원)이고,
              null이면(검토 미수행·만료·초기화) `table`은 마지막 영속 상태입니다.
              `stage=scenario_validated`인데 `validation=null`이면 검토가 만료된 것 — 재검토가 필요합니다.

            <br><br>
            ### Signpost 이후 단계 합성 (B.E)
            - `refinementState.scenarioGroupMeta`: 그룹명/요약/설명 — 생성 시 Agent가 채우며, 확정 화면의 편집 초기값입니다
            - `table.scenarios[].signposts[]`: 행별 Signpost 목록 (stage가 signpost_created/completed일 때 DB 합성) —
              확정 요청의 signposts 재투영에 이 데이터를 사용합니다

            <br><br>
            ### 화면 개념 - 응답 필드 매핑
            - **이벤트 리스트** (화면 상단): `baseEvents[]` — 그룹을 구성하는 이벤트 (표시 순서 보존, B.E DB 합성)
            - **분기항목** (표 헤더): `table.dimensionColumns[]` — `key`/`label` + (i) 아이콘용 `description`/`rationale`
            - **분기값** (헤더별 선택 가능한 값 전체): `dimensionValueOptions{key}` — 드롭다운 데이터 (`valueId`/`label`/`rationale`)
            - **분기기준** (시나리오 행별 현재 값): `table.scenarios[].dimensions[]` — `value`(표시 label)와
              선택 상태 매칭용 `valueId`, 셀 (i) 아이콘용 `rationale`
            - **배지**: `table.scenarios[].scenarioRole`(floor/ceiling/mixed) + `pressureBand`(5단계) — 라벨 필드 동봉, v1 legacy는 null
            - **근거 마크다운**: `evidence.eventAnalysisMarkdown`(Event 분석), `evidence.roleSelectionMarkdown`(대표 시나리오 선정 근거) — sanitize 후 렌더
            - **진행/실패 상태**: `creationState` — `operationStatus`(+라벨) / `currentPhase`(+라벨). Agent가 새 어휘를 추가하면 null.
              terminal 여부는 SSE로 알 수 있으므로 이 API는 폴링 용도가 아니라 표시 용도입니다.
              failed/interrupted면 `creationState.lastError`(B.E DB 합성)로 실패 사유를 표시합니다.

            <br><br>
            ### 편집 요청 왕복 규칙
            검토/Signpost 요청 시에는 headers(key/label/displayOrder)와 rows(scenarioId/displayOrder/title/cells)만 보내고
            **cells에는 `dimensions[].value`(표시 label)를 그대로 재전송**합니다. `valueId`/`rationale` 등 근거 필드는 전송하지 않습니다.
            드롭다운에서 다른 분기값을 선택한 경우 `dimensionValueOptions`의 해당 항목 `label`을 cells 값으로 사용합니다.

            <br><br>
            ### 참고
            - 생성 진행 중에는 준비된 섹션만 채워집니다 — 표가 아직 없으면 `table: null`, 마크다운은 준비 전이면 null입니다.
            - legacy(v1) 그룹도 렌더됩니다 (creationState는 null, 근거 필드는 비어있음).
            - `operationId`를 주면 해당 작업 기준의 중간 스냅샷을 조회합니다 (생략 시 최신).
            - Agent 오류는 B.E-Agent 구간 문제로 취급해 500 + API-004(외부 API 오류 표준 메시지)로 내려갑니다 (원문은 서버 로그).
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

    @ApiOrder(4)
    @Operation(
        summary = "추가 분기항목 후보 조회 API",
        description = """
            step1 편집에서 + 버튼으로 추가할 수 있는 분기항목 후보 목록을 `폴링` 조회합니다. <br>
            후보는 그룹 생성 중 Agent가 background에서 비동기로 생성하므로 이 API는 **조회 전용**입니다.
            호출해도 후보가 새로 생성되거나 선택 상태가 바뀌지 않습니다. <br>

            <br><br>
            ### 폴링 규칙 (`generationStatus` 기준)
            - `queued`/`running`: 생성 중 — 일정 간격 후 재조회
            - `ready`: `candidates[]` 사용. **빈 배열이어도 정상**(추가 가능한 후보 없음)
            - `not_requested`/`failed`/`interrupted`/`discarded`: 폴링 종료, 후보 영역 비활성

            <br><br>
            ### 채택 흐름
            후보를 채택하면 `candidates[].key`를 표 헤더 key로, `values[].label`을 셀 값으로 사용해 기존 검토/Signpost 왕복에 그대로 포함합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공 (후보 생성 진행/실패 상태도 200으로 반환)")
    @ApiResponse(responseCode = "400", description = "시나리오 그룹 없음 (B.E 선검증)")
    @ApiResponse(responseCode = "500", description = "Agent 오류 — v4 그룹 아님/분기항목 변경 불가 단계 포함. API-004(외부 API 오류 표준 메시지)로 내려가며 원문은 서버 로그에 남습니다")
    @GetMapping("/{scenarioGroupId}/design/additional-dimensions")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<AdditionalDimensionsRes> getAdditionalDimensions(
        @Parameter(description = "시나리오 그룹 ID", example = "194") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(scenarioDesignV4Service.getAdditionalDimensions(scenarioGroupId));
    }

    @ApiOrder(5)
    @Operation(
        summary = "시나리오 검토/Signpost 생성 API (SSE)",
        description = """
            시나리오 검토 및 Signpost 생성 요청의 **완료 신호를 수신하는 구독 SSE**입니다. <br>
            시나리오 검토와 Signpost 생성은 비동기라 요청 API의 응답은 접수일 뿐이며, 실제 결과는 Agent 처리 완료 시 해당 SSE 연결로 푸시됩니다.

            <br><br>
            ### 연결 시점
            [멀티 이벤트 기반 시나리오 그룹 생성 API]가 `completed`로 종료되면 **곧바로 이 API를 연결**합니다. <br>
            이후 사용자의 검토/Signpost 생성 요청 결과가 이 연결로만 전달됩니다. <br>
            편집 중인 그룹(확정 전)의 설계 화면에 재진입할 때도 먼저 연결합니다.

            <br><br>
            ### 연결 종료
            **확정 성공 또는 설계 화면 이탈 시 F.E가 연결을 닫습니다** (`EventSource.close()`) — 구독 채널이라 서버가 닫아도 자동 재연결되므로 종료는 F.E 책임입니다.
            닫지 않은 연결은 서버가 30분 타임아웃으로 정리합니다.

            <br><br>
            ### 전송 형식
            GET 방식이므로 브라우저 `EventSource`로 구독하며, 자동 재연결이 지원됩니다. <br>
            `EventSource` 대신 fetch 등으로 직접 연결하는 경우 **Accept 헤더는 `text/event-stream, application/json`으로 보냅니다**
            (접수 실패 오류가 JSON으로 응답되므로 — 생성 스트림과 동일 규칙). <br>
            `text/event-stream`. 각 프레임은 이벤트 이름(`event:`)과 JSON 페이로드(`data:`)로 구성됩니다. <br>
            연결 유지를 위해 약 15초마다 keep-alive 주석 프레임(`: keep-alive`)이 전달됩니다 — 생성 스트림과 동일 형식. (연결 생존 신호로만 취급(무시)합니다.)

            <br><br>
            ### 이벤트 종류 & 처리 가이드
            페이로드는 **신호만** 담습니다 — 수신 즉시 [설계 화면 데이터 조회 API]를 재조회해 화면을 갱신합니다.

            **(1) `connected`** — 연결 직후 1회. 이때 [설계 화면 데이터 조회 API]를 최초 1회 조회합니다.
            ```
            event: connected
            data: {"scenarioGroupId":12345}
            ```

            **(2) `validation`** — 검토 결과 도착. 재조회하면 검토 통과 편집본이 `table`에 오버레이되고, `refinementState.validation`(isValid/reason)이 채워집니다.
            - 통과: `stage` = `scenario_validated` / 실패: `stage` = `scenario_created` 유지
            ```
            event: validation
            data: {"scenarioGroupId":12345,"type":"validation","stage":"scenario_validated"}
            ```

            **(3) `signposts`** — Signpost 생성 완료. 재조회하면 편집본이 영속화된 표에 narrative/확률/`signposts[]`/`scenarioGroupMeta`가 채워져 내려옵니다.
            ```
            event: signposts
            data: {"scenarioGroupId":12345,"type":"signposts","stage":"signpost_created","result":"success"}
            ```

            **(4) 실패** — Agent 작업 실패. 이벤트 이름은 실패한 단계(`validation`/`signposts`)와 동일하며 `result=failure` + `reason`이 실립니다. (stage 없음 — 상태 미전이)
            오류 표시 후 재요청을 허용합니다. 미해결 실패는 연결(재연결) 시점에 재전송되므로 푸시를 놓쳐도 재연결로 복구됩니다.
            ```
            event: validation
            data: {"scenarioGroupId":12345,"type":"validation","result":"failure","reason":"<에러 메시지>"}
            ```

            <br><br>
            ### 페이로드 필드
            - `scenarioGroupId`(number): 설계 세션 식별자
            - `type`(string): `validation` / `signposts`
            - `stage`(string): `scenario_created` / `scenario_validated` / `signpost_created` (실패 시 없음)
            - `result`(string): `success` / `failure`
            - `reason`(string): 실패 사유 (`result=failure`일 때만)
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream)")
    @GetMapping(value = "/{scenarioGroupId}/refinement/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter streamScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return sseRegistry.connect(scenarioGroupId);
    }

    @ApiOrder(6)
    @Operation(
        summary = "시나리오 변경사항 검토 요청 API",
        description = """
            step1에서 사용자가 편집한 분기항목/분기기준/시나리오를 Agent에 검토 요청합니다. <br>
            **검토를 요청해도 Agent는 실제 DB에 반영하지 않습니다.** <br>
            검토 통과 시 stage가 `scenario_validated`로 전이되고, 실패 시 `scenario_created`를 유지하며 사유가 전달됩니다. <br>

            <br><br>
            ### 요청 본문
            - `table.dimensionColumns[]`: 편집 후 분기항목(헤더) 목록 (key, label)
            - `table.scenarios[]`: 편집 후 최종 시나리오 집합
              - 미채번 행(`scenarioId` null) = 신규 추가, 요청에서 빠진 행 = 삭제
              - 행: `scenarioName` / `dimensions[]`(key, value) / `displayOrder` (신규 분기항목 셀은 value null → Agent 자동생성)
            """
    )
    @ApiResponse(responseCode = "200", description = "검토 요청 접수")
    @ApiResponse(responseCode = "403", description = "다른 사용자가 진행 중인 설계 세션 (진행자만 편집 가능)")
    @PostMapping("/{scenarioGroupId}/design/validation")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> validateScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioValidationReq request
    ) {
        return BaseResponseFactory.create(scenarioDesignService.validateDesign(scenarioGroupId, request));
    }

    @ApiOrder(7)
    @Operation(
        summary = "Signpost 생성 API (step2 진입)",
        description = """
            step1을 종료하고 step2(One-view 검토)로 진입합니다. <br>
            **편집본 전체 table을 Agent에 전달하며, 이 시점에 시나리오 그룹/시나리오 변경 데이터가 DB에 영속화됩니다** (UPDATE / 신규 INSERT·scenarioId 채번 / 빠진 행 soft delete). <br>
            이어서 Agent가 시나리오별 Signpost / Narrative / 초기확률 / 그룹명·설명을 산출하며, stage가 `signpost_created`로 업데이트 됩니다. <br>
            산출 결과는 Signpost 콜백 후 SSE 푸시 → [설계 화면 데이터 조회 API] 재조회로 받아갑니다. <br>

            <br><br>
            ### 요청 본문
            - `table.dimensionColumns[]` + `table.scenarios[]`: 편집 후 최종 시나리오 표
              - 행: `scenarioName` / `dimensions[]` / `displayOrder` (검토 후 추가한 신규 시나리오는 `scenarioId` null, 빠진 행 = 삭제)
              - narrative/확률/baseCase/Signpost는 받지 않음 (Agent 산출)
            """
    )
    @ApiResponse(responseCode = "200", description = "단계2 진입 요청 접수")
    @ApiResponse(responseCode = "403", description = "다른 사용자가 진행 중인 설계 세션 (진행자만 편집 가능)")
    @PostMapping("/{scenarioGroupId}/design/signposts")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> createScenarioSignposts(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioSignpostsReq request
    ) {
        return BaseResponseFactory.create(scenarioDesignService.createSignposts(scenarioGroupId, request));
    }

    @ApiOrder(8)
    @Operation(
        summary = "시나리오 설계 최종 확정 API",
        description = """
            step2(One-view 검토)를 종료하고 시나리오 설계를 최종 확정합니다. <br>
            step2 편집(그룹명/설명/Narrative/가능성/Signpost)을 한 번에 전달해 적용+확정합니다. <br>
            stage가 `completed`, status가 `created`로 변경되며, 단계별 스냅샷(초기값/최종값)이 저장됩니다. <br>

            <br><br>
            ### 요청 본문
            - `scenarioGroup`: 그룹 `title` / `summary` / `description` (확정에서만 편집)
            - `table.dimensionColumns[]` + `table.scenarios[]`: full 표
              - 행: `scenarioName` / `dimensions[]` / `displayOrder` + `baseCase` / `probability` / `narrative`
              - `signposts[]`: `signpostId` / `label` / `direction` / `rationale` / `strength` / `displayOrder` / `used`
            """
    )
    @ApiResponse(responseCode = "200", description = "확정 성공")
    @ApiResponse(responseCode = "403", description = "다른 사용자가 진행 중인 설계 세션 (진행자만 편집 가능)")
    @PostMapping("/{scenarioGroupId}/design/confirmation")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> confirmScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioConfirmationReq request
    ) {
        return BaseResponseFactory.create(scenarioDesignService.confirmDesign(scenarioGroupId, request));
    }

    @ApiOrder(9)
    @Operation(
        summary = "시나리오 설계 전체 초기화 API",
        description = """
            '전체 초기화' — 사용자 편집/검토 결과(Redis 임시 캐시)를 버리고 마지막 영속 상태로 되돌립니다. <br>
            응답 본문은 없으며, **초기화 성공 후 [설계 화면 데이터 조회 API]를 재조회**해 화면을 갱신합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @ApiResponse(responseCode = "403", description = "다른 사용자가 진행 중인 설계 세션 (진행자만 편집 가능)")
    @PostMapping("/{scenarioGroupId}/design/reset")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> resetScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        scenarioDesignService.reset(scenarioGroupId);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

}
