package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.sse.ScenarioDesignSseRegistry;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Profile("!dedicated")
@Tag(name = "03. Next Gen MI Scenario Design API", description = "Next Gen MI 시나리오 설계 파이프라인 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioDesignController {

    private final ScenarioDesignSseRegistry sseRegistry;

    @ApiOrder(0)
    @Operation(
        summary = "시나리오 설계 세션 SSE 구독 API",
        description = """
            설계 세션(scenarioGroupId)의 진행 이벤트를 SSE로 구독합니다. <br>
            생성/검토/Signpost 콜백이 도착하면 "갱신됨" 신호(stage)를 푸시하며, **FE는 수신 후 세션 상태 조회 API로 재조회**합니다. <br>
            연결 직후 `connected` 이벤트가 1회 전송됩니다. <br>

            push 유실 대비: 재연결 시 재조회 + 폴백 폴링으로 수렴합니다. <br>

            <br><br>
            ### 전송 형식
            `text/event-stream`. 각 프레임은 이벤트 이름(`event:`)과 JSON 페이로드(`data:`)로 구성됩니다. <br>

            <br><br>
            ### 이벤트 종류 & 페이로드
            **(1) `connected`** — 연결 직후 1회. 핸드셰이크 신호이며 이때 FE는 최초 상태를 재조회합니다.
            ```
            event: connected
            data: {"scenarioGroupId":12345}
            ```

            **(2) `generation`** — 생성 콜백 도착 (단계1 시나리오 생성 완료)
            ```
            event: generation
            data: {"scenarioGroupId":12345,"type":"generation","stage":"scenario_created"}
            ```

            **(3) `validation`** — 검토 콜백 도착. **결과에 따라 `stage`가 분기됩니다.**
            - 통과: `stage` = `scenario_validated`
            - 실패: `stage` = `scenario_created`
            ```
            event: validation
            data: {"scenarioGroupId":12345,"type":"validation","stage":"scenario_validated"}
            ```
            ```
            event: validation
            data: {"scenarioGroupId":12345,"type":"validation","stage":"scenario_created"}
            ```

            **(4) `signposts`** — Signpost 콜백 도착 (단계2 signpost 생성 완료)
            ```
            event: signposts
            data: {"scenarioGroupId":12345,"type":"signposts","stage":"signpost_created"}
            ```

            <br><br>
            ### 페이로드 필드
            - `scenarioGroupId`(number): 설계 세션 식별자
            - `type`(string): 트리거 콜백 종류 — `generation` / `validation` / `signposts`
            - `stage`(string): stage 종류 — `scenario_created` / `scenario_validated` / `signpost_created`

            <br><br>
            ### 처리 가이드
            - 페이로드는 **신호만** 담습니다. 수신 즉시 `GET /{scenarioGroupId}/design`로 재조회해 화면을 갱신합니다.
            - 연결이 끊기면(타임아웃 30분 등) 재연결 후 `connected` 수신 시 다시 재조회합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream)")
    @GetMapping(value = "/{scenarioGroupId}/design/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter streamScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return sseRegistry.connect(scenarioGroupId);
    }


    @ApiOrder(1)
    @Operation(
        summary = "시나리오 설계 시작 API (step1 진입)",
        description = """
            DESIGNABLE 상태인 이벤트에 대해 시나리오 설계(단계1)를 시작합니다. <br>
            요청 사용자가 해당 이벤트를 점유(lock)하며, Agent에 시나리오 그룹/시나리오 생성을 요청합니다. <br>
            이미 다른 사용자가 설계 중인 이벤트면 점유 충돌로 거부됩니다. <br>

            <br><br>
            ### 요청 본문
            - `miEventId`: 설계 대상 이벤트 ID

            <br><br>
            ### 응답 필드
            - `scenarioGroupId`, `status`(`creating`), `stage`(`ready`)
            """
    )
    @ApiResponse(responseCode = "200", description = "설계 시작 성공")
    @PostMapping
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> startScenarioDesign(
        @Valid @RequestBody ScenarioDesignStartReq request
    ) {
        // TODO: 설계 시작(점유 획득 + Agent 생성요청 프록시) 구현 예정
        return null;
    }

    @ApiOrder(2)
    @Operation(
        summary = "시나리오 설계 세션 상태 조회 API",
        description = """
            시나리오 설계(단계1/2)의 현재 세션 상태를 조회합니다. <br>
            '전체 초기화'는 reset API로 검토 캐시를 비운 뒤 이 API로 DB 생성 초기값을 받습니다. <br>

            <br><br>
            ### 소스
            - 생성 초기값(step1) / signpost(step2) / 확정 = **DB**
            - 검토 결과(Agent 자동생성값 포함 table + isValid/reason) = **Redis 키(휘발, TTL)** — 있으면 step1을 오버레이, 없으면(미검토/초기화/만료) DB 생성 초기값

            <br><br>
            ### 응답 필드 (기존 그룹 화면과 동일 컨벤션으로 변환해 내림)
            - `scenarioGroupId`
            - `status` + `statusLabel` (creating/created/realized)
            - `stage` + `stageLabel` (ready / scenario_created / scenario_validated / signpost_created / completed)
            - `occupied` / `occupantUserId`: 점유 정보
            - `step1`: 분기항목/시나리오 표 (현 단계 해당분, 아니면 null)
              - `dimensionColumns[]`(key, label) + `scenarios[]`
              - 행: `scenarioId`(신규면 null) / `isNew` / `scenarioName` / `baseCase` / `dimensions[]`(key, label, value, valueLabel) / `displayOrder`
            - `step2`: One-view (현 단계 해당분, 아니면 null)
              - `groupName` / `summary` / `description` + `table`(step1과 동일 구조)
              - 행에 `probability` / `narrative` / `signposts[]`(label, direction+directionLabel, probabilityImpact, strength+strengthLabel, rationale) 추가 채움
            - `validation`: 검토 결과 (`isValid`, `reason`, 미수행 시 null)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/design")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStateRes> getScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        // TODO: 설계 세션 상태 조회 구현 예정 (Redis 검토캐시 우선, 없으면 DB)
        return null;
    }

    @ApiOrder(3)
    @Operation(
        summary = "시나리오 분기항목/분기기준 템플릿 조회 API",
        description = """
            단계1에서 분기항목 추가 및 분기기준값 선택 시 사용할 템플릿 목록을 조회합니다. <br>
            선택된 이벤트 유형(event_type_l2)에 정의된 기준입니다. <br>

            <br><br>
            ### 응답 필드
            - `dimensions[]`: `dimKey`, `label`, `required`, `displayOrder`, `valueEnums[]`(선택 가능한 분기기준값 목록)
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/dimension-templates")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDimensionTemplateRes> getScenarioDimensionTemplates(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        // TODO: 분기항목/분기기준 템플릿 조회 구현 예정
        return null;
    }

    @ApiOrder(4)
    @Operation(
        summary = "시나리오 변경사항 검토 요청 API",
        description = """
            단계1에서 사용자가 편집한 분기항목/분기기준/시나리오를 Agent에 검토 요청합니다. <br>
            **검토 단계에서는 DB에 반영하지 않습니다.** <br>
            검토 통과 시 stage가 `scenario_validated`로 전이되고, 실패 시 `scenario_created`를 유지하며 사유가 전달됩니다. <br>

            <br><br>
            ### 요청 본문 (조회 응답과 동일 FE 구조 / 이 단계 편집 가능 필드만)
            - `table.dimensionColumns[]`: 편집 후 분기항목(헤더) 목록 (key, label)
            - `table.scenarios[]`: 편집 후 최종 시나리오 집합
              - 미채번 행(`scenarioId` null) = 신규 추가, 요청에서 빠진 행 = 삭제
              - 행: `scenarioName` / `dimensions[]`(key, value) / `displayOrder` (신규 분기항목 셀은 value null -> Agent 자동생성)
              - narrative/확률/baseCase/Signpost는 이 단계에서 받지 않음 (확정 단계 또는 Agent 산출)
            """
    )
    @ApiResponse(responseCode = "200", description = "검토 요청 접수")
    @PostMapping("/{scenarioGroupId}/design/validation")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> validateScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioValidationReq request
    ) {
        // TODO: 검토 요청(편집 table Agent 프록시) 구현 예정
        return null;
    }

    @ApiOrder(5)
    @Operation(
        summary = "Signpost 생성 API (step2 진입)",
        description = """
            단계1을 종료하고 단계2(One-view 검토)로 진입합니다. <br>
            **편집본 전체 table을 Agent에 전달하며, 이 시점에 시나리오 그룹/시나리오가 DB에 영속화됩니다** (UPDATE / 신규 INSERT·scenarioId 채번 / 빠진 행 soft delete). <br>
            이어서 Agent가 시나리오별 Signpost / Narrative / 초기확률 / 그룹명·설명을 산출하며, stage가 `signpost_created`로 업데이트 됩니다. <br>
            산출 결과는 Signpost 콜백 후 SSE 푸시 -> 세션 조회로 받아갑니다. <br>

            <br><br>
            ### 요청 본문 (검토와 동일한 편집 표 / 이 단계 편집 가능 필드만)
            - `table.dimensionColumns[]` + `table.scenarios[]`: 편집 후 최종 시나리오 표
              - 행: `scenarioName` / `dimensions[]` / `displayOrder` (검토 후 추가한 신규 시나리오는 `scenarioId` null, 빠진 행 = 삭제)
              - narrative/확률/baseCase/Signpost는 받지 않음 (Agent 산출)
            """
    )
    @ApiResponse(responseCode = "200", description = "단계2 진입 요청 접수")
    @PostMapping("/{scenarioGroupId}/design/signposts")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> createScenarioSignposts(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioSignpostsReq request
    ) {
        // TODO: 단계1 완료/단계2 진입(편집본 영속화 + Signpost 산출 Agent 프록시) 구현 예정
        return null;
    }

    @ApiOrder(6)
    @Operation(
        summary = "시나리오 설계 최종 확정 API",
        description = """
            단계2(One-view 검토)를 종료하고 시나리오 설계를 최종 확정합니다. <br>
            단계2 편집(그룹명/설명/Narrative/가능성/Signpost)을 한 번에 전달해 적용+확정합니다. <br>
            stage가 `completed`, status가 `created`로 변경되며, 단계별 스냅샷(초기값/최종값)이 저장됩니다. <br>

            <br><br>
            ### 요청 본문 (full 편집 / 이 단계는 전부 편집 가능)
            - `scenarioGroup`: 그룹 `title` / `summary` / `description` (확정에서만 편집)
            - `table.dimensionColumns[]` + `table.scenarios[]`: full 표
              - 행: `scenarioName` / `dimensions[]` / `displayOrder` + `baseCase` / `probability` / `narrative`
              - `signposts[]`: `signpostId` / `label` / `direction` / `rationale` / `strength` / `displayOrder` / `used`(사용여부 토글)
            """
    )
    @ApiResponse(responseCode = "200", description = "확정 성공")
    @PostMapping("/{scenarioGroupId}/design/confirmation")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStageRes> confirmScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioConfirmationReq request
    ) {
        // TODO: 최종 확정(Agent 프록시 + 스냅샷 저장 + 검토캐시 정리) 구현 예정
        return null;
    }

    @ApiOrder(7)
    @Operation(
        summary = "시나리오 설계 전체 초기화 API",
        description = """
            단계1 '전체 초기화' — 사용자 편집/검토 결과를 버리고 Agent 생성 초기값으로 되돌립니다. <br>
            검토 결과(Redis 임시 캐시)를 삭제하므로, 이후 세션 조회는 DB의 생성 초기값(마지막 적용 상태)을 반환합니다. <br>
            (확정/이탈 시점의 검토 캐시 정리에도 동일 로직을 재사용합니다) <br>

            <br><br>
            ### 응답 필드
            - 삭제 후 DB 기준 세션 상태 (세션 상태 조회 API와 동일 형태)
            """
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @PostMapping("/{scenarioGroupId}/design/reset")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ScenarioDesignStateRes> resetScenarioDesign(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        // TODO: 검토 캐시(Redis 키) 삭제 + DB 기준 상태 반환 구현 예정
        return null;
    }

}
