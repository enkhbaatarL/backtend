package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.ScenarioDesignService;
import com.sk.domain.mi.service.nextgen.sse.registry.ScenarioDesignSseRegistry;
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
@Tag(name = "03-1. Next Gen MI Scenario Design API", description = "Next Gen MI 시나리오 설계 파이프라인 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioDesignController {

    private final ScenarioDesignSseRegistry sseRegistry;
    private final ScenarioDesignService scenarioDesignService;

    @ApiOrder(0)
    @Operation(
        summary = "시나리오 설계 세션 SSE 구독 API",
        description = """
            설계 세션(scenarioGroupId)의 진행 이벤트를 SSE로 구독합니다. <br>
            Agent로부터 `시나리오 생성`, `시나리오 검토`, `Signpost 생성` 콜백이 도착하면 신호(stage)를 푸시하며, **FE는 수신 후 세션 상태 조회 API로 재조회**합니다. <br>
            연결 직후 `connected` 이벤트가 1회 전송됩니다. <br>

            push 유실 대비: 재연결 시 재조회 + 폴백 폴링으로 수렴합니다. <br>

            <br><br>
            ### 전송 형식
            `text/event-stream`. 각 프레임은 이벤트 이름(`event:`)과 JSON 페이로드(`data:`)로 구성됩니다. <br>
            연결 유지를 위해 서버가 약 20초마다 keep-alive 주석 프레임(`:hb`)을 보냅니다. 주석 프레임이므로 `EventSource` 핸들러는 무시하며 FE에서 별도 처리할 필요가 없습니다. <br>

            <br><br>
            ### 이벤트 종류 & 페이로드
            **(1) `connected`** — 연결 직후 1회. 핸드셰이크 신호이며 이때 FE는 최초 상태를 재조회합니다.
            ```
            event: connected
            data: {"scenarioGroupId":12345}
            ```

            **(2) `generation`** — 시나리오 생성 콜백 도착 (단계1 시나리오 생성 완료)
            ```
            event: generation
            data: {"scenarioGroupId":12345,"type":"generation","stage":"scenario_created"}
            ```

            **(3) `validation`** — 시나리오 검토 콜백 도착. **결과에 따라 `stage`가 분기됩니다.**
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
            data: {"scenarioGroupId":12345,"type":"signposts","stage":"signpost_created","result":"success"}
            ```

            **(5) 실패** — Agent 작업 실패 시. **이벤트 이름은 실패한 단계(generation/validation/signposts)와 동일**하되 `result=failure` + `reason`이 실린다. (stage 없음 — DB 미전이)
            ```
            event: generation
            data: {"scenarioGroupId":12345,"type":"generation","result":"failure","reason":"<에러 메시지>"}
            ```

            <br><br>
            ### 페이로드 필드
            - `scenarioGroupId`(number): 설계 세션 식별자
            - `type`(string): 트리거 콜백 종류 — `generation` / `validation` / `signposts`
            - `stage`(string): stage 종류 — `scenario_created` / `scenario_validated` / `signpost_created` (실패 시 없음)
            - `result`(string): `success` / `failure`
            - `reason`(string): 실패 사유 (`result=failure`일 때만)

            <br><br>
            ### 처리 가이드
            - 성공 페이로드는 **신호만** 담습니다. 수신 즉시 `GET /{scenarioGroupId}/design`로 재조회해 화면을 갱신합니다.
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
            `DESIGNABLE` 상태인 이벤트에 대해 시나리오 설계(단계1)를 시작합니다. <br>
            요청 사용자가 해당 이벤트를 점유(lock)하며, Agent에 시나리오 그룹/시나리오 생성을 요청합니다. <br>
            이미 다른 사용자가 설계 중인 이벤트면 점유 충돌로 거부됩니다. <br>
            **비동기**: 설계 시작 시 `stage=ready`로 응답하고, **생성 완료는 generation 콜백(SSE) 수신 후 세션 재조회로 반영**됩니다. <br>

            <br><br>
            ### 요청 본문
            - `miEventId`: 설계 대상 이벤트 ID

            <br><br>
            ### 응답 필드 (접수 시점)
            - `scenarioGroupId`
            - `status` + `statusLabel`(creating), `stage` + `stageLabel`(ready) — 생성 완료 후 SSE -> 재조회로 `scenario_created` 전이
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
        return BaseResponseFactory.create(scenarioDesignService.startDesign(request));
    }

    @ApiOrder(2)
    @Operation(
        summary = "시나리오 설계 세션 상태 조회 API",
        description = """
            시나리오 설계(단계1/단계2)의 현재 세션 상태를 조회합니다. <br>
            '전체 초기화'는 reset API로 검토(validation)된 데이터 캐시를 비운 뒤, 해당 API로 DB 생성 초기값을 받습니다. <br>

            <br><br>
            ### 응답 필드
            - `scenarioGroupId`
            - `status` + `statusLabel` (creating/created/realized)
            - `stage` + `stageLabel` (ready / scenario_created / scenario_validated / signpost_created / completed)
            - `occupied` / `occupantUserId`: 점유 정보
            - `step1`: 분기항목/분기기준 값 시나리오 표
              - `dimensionColumns[]`(key, label) + `scenarios[]`
              - 행: `scenarioId`(신규면 null) / `isNew` / `scenarioName` / `baseCase` / `dimensions[]`(key, label, value, valueLabel) / `displayOrder`
            - `step2`: One-view
              - `groupName` / `summary` / `description` + `table`
              - 행에 `probability` / `narrative` / `signposts[]`(label, direction+directionLabel, probabilityImpact, strength+strengthLabel, rationale)
            - `validation`: 검토 결과 (`isValid`, `reason`)
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
        return BaseResponseFactory.create(scenarioDesignService.getDesign(scenarioGroupId));
    }

    @ApiOrder(3)
    @Operation(
        summary = "시나리오 분기항목/분기기준 템플릿 조회 API",
        description = """
            step1에서 분기항목 추가 및 분기기준값 선택 시 사용할 템플릿 목록을 조회합니다. <br>
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
        return BaseResponseFactory.create(scenarioDesignService.getDimensionTemplates(scenarioGroupId));
    }

    @ApiOrder(4)
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
              - 행: `scenarioName` / `dimensions[]`(key, value) / `displayOrder` (신규 분기항목 셀은 value null -> Agent 자동생성)
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
        return BaseResponseFactory.create(scenarioDesignService.validateDesign(scenarioGroupId, request));
    }

    @ApiOrder(5)
    @Operation(
        summary = "Signpost 생성 API (step2 진입)",
        description = """
            step1을 종료하고 step2(One-view 검토)로 진입합니다. <br>
            **편집본 전체 table을 Agent에 전달하며, 이 시점에 시나리오 그룹/시나리오 변경 데이터가 DB에 영속화됩니다** (UPDATE / 신규 INSERT·scenarioId 채번 / 빠진 행 soft delete). <br>
            이어서 Agent가 시나리오별 Signpost / Narrative / 초기확률 / 그룹명·설명을 산출하며, stage가 `signpost_created`로 업데이트 됩니다. <br>
            산출 결과는 Signpost 콜백 후 SSE 푸시 -> 세션 조회로 받아갑니다. <br>

            <br><br>
            ### 요청 본문
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
        return BaseResponseFactory.create(scenarioDesignService.createSignposts(scenarioGroupId, request));
    }

    @ApiOrder(6)
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

    @ApiOrder(7)
    @Operation(
        summary = "시나리오 설계 전체 초기화 API",
        description = """
            step1 '전체 초기화' — 사용자 편집/검토 결과를 버리고 Agent 생성 초기값으로 되돌립니다. <br>
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
        return BaseResponseFactory.create(scenarioDesignService.reset(scenarioGroupId));
    }

}
