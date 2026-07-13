package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationEditReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationGenerateRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.sse.registry.PriceInterpretationSseRegistry;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 시나리오별 가격전망 해석 API.
 * 시나리오 그룹 상세의 '시나리오별 가격전망 해석' section을 다룬다.
 * 해석 조회는 그룹 상세 API에 embed된다.
 */
@Profile("!dedicated")
@Tag(name = "04-1. Next Gen MI Price Interpretation API", description = "Next Gen MI 시나리오별 가격전망 해석 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class PriceInterpretationController {

    private final PriceInterpretationSseRegistry sseRegistry;
    private final PriceInterpretationService priceInterpretationService;

    @ApiOrder(0)
    @Operation(
        summary = "가격전망 해석 SSE 구독 API",
        description = """
            시나리오 그룹 상세 화면에서 `가격전망 해석` 생성 진행 이벤트를 SSE로 구독합니다. <br>
            Agent 콜백이 도착하면 신호를 푸시하며, **FE는 수신 후 `가격전망 해석 조회 API`로 재조회**하여 화면을 갱신합니다. <br><br>

            연결 직후 `connected` 이벤트가 1회 전송됩니다. <br>

            <br><br>
            ### 전송 형식
            `text/event-stream`. 각 프레임은 이벤트 이름(`event:`)과 JSON 페이로드(`data:`)로 구성됩니다. <br>
            연결 유지를 위해 서버가 약 20초마다 keep-alive 주석 프레임(`:hb`)을 보냅니다. <br>

            <br><br>
            ### 이벤트 종류 & 페이로드
            **(1) `connected`** — 연결 직후 1회. 핸드셰이크 신호입니다.
            ```
            event: connected
            data: {"scenarioGroupId":1}
            ```

            **(2) `generation`** — 가격전망 해석 생성 콜백 도착 (성공)
            ```
            event: generation
            data: {"scenarioGroupId":1,"type":"generation","result":"success"}
            ```

            **(3) 실패** — Agent 생성 실패 시. `result=failure` + `reason`이 포함됩니다. (대상 시나리오는 failed 상태)
            ```
            event: generation
            data: {"scenarioGroupId":1,"type":"generation","result":"failure","reason":"<에러 메시지>"}
            ```

            <br><br>
            ### 처리 가이드
            - 성공 페이로드는 **신호만** 담습니다. 수신 즉시 `GET /{scenarioGroupId}/price-interpretations`로 재조회해 해석을 갱신합니다.
            - 연결이 끊기면 재연결 후 `connected` 수신 시 다시 재조회합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream)")
    @GetMapping(value = "/{scenarioGroupId}/price-interpretations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter streamPriceInterpretation(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return sseRegistry.connect(scenarioGroupId);
    }

    @ApiOrder(1)
    @Operation(
        summary = "가격전망 해석 조회 API",
        description = """
            시나리오 그룹의 '시나리오별 가격전망 해석' section만 경량으로 조회합니다. <br>
            **SSE 신호(generation) 수신 후 재조회 용도** - 무거운 그룹 상세 전체 대신 사용함 <br>
            (최초 진입 시에는 시나리오 그룹 상세 응답의 embed된 section을 사용하고, 이후 실시간 갱신에만 이 API를 사용) <br>

            <br><br>
            ### 응답 필드
            - `generatable`: '가격전망 해석' 버튼 활성 여부 (수급모델 실행 완료 AND (해석 미보유 OR 수급모델 재기동))
            - `interpretations[]`: 해석 보유 시나리오 — `scenarioId`, `scenarioName`, `baseCase`, `displayOrder`, `status`, `content`, `revision`, `updatedAt`
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{scenarioGroupId}/price-interpretations")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PriceInterpretationSectionRes> getPriceInterpretations(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(priceInterpretationService.getInterpretations(scenarioGroupId));
    }

    @ApiOrder(2)
    @Operation(
        summary = "가격전망 해석 생성 API",
        description = """
            수급모델 실행이 완료됐지만 아직 해석이 없는 시나리오들 또는 수급모델 실행 결과가 갱신된 시나리오들을 대상으로 가격전망 해석 생성을 시작합니다. <br>
            **비동기**: 대상 시나리오를 `generating`으로 생성하고 Agent에 산출을 요청한 뒤 응답합니다. <br>
            생성 결과는 generation 콜백(SSE) 수신 후 재조회로 반영됩니다. <br>

            <br><br>
            ### 응답 필드
            - `scenarioGroupId`
            - `targetScenarios[]`: 이번 생성 대상 시나리오 목록
              - `scenarioId` / `status` + `statusLabel`: 접수 시점 상태 (항상 `generating`/생성중)
            """
    )
    @ApiResponse(responseCode = "200", description = "생성 접수 성공")
    @PostMapping("/{scenarioGroupId}/price-interpretations")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PriceInterpretationGenerateRes> generatePriceInterpretation(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(priceInterpretationService.generateInterpretation(scenarioGroupId));
    }

    @ApiOrder(3)
    @Operation(
        summary = "가격전망 해석 수정 API",
        description = """
            시나리오의 가격전망 해석을 사용자가 직접 수정합니다. <br>
            생성 중(`generating`)인 시나리오는 수정할 수 없습니다. <br>

            <br><br>
            ### 요청 본문
            - `content`: 수정할 해석 내용 (최대 2000자)
            """
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PatchMapping("/{scenarioGroupId}/scenarios/{scenarioId}/price-interpretation")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PriceInterpretationRes> editPriceInterpretation(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Parameter(description = "시나리오 ID", example = "11") @PathVariable Long scenarioId,
        @Valid @RequestBody PriceInterpretationEditReq request
    ) {
        return BaseResponseFactory.create(priceInterpretationService.editInterpretation(scenarioGroupId, scenarioId, request));
    }

    @ApiOrder(4)
    @Operation(
        summary = "가격전망 해석 초기화 API",
        description = """
            시나리오의 가격전망 해석을 Agent 생성 원본으로 복원합니다. (Agent 재호출 아님) <br>
            생성 중(`generating`)이거나 성공한 Agent 원본이 없으면 초기화할 수 없습니다. <br>

            <br><br>
            ### 응답 필드
            - 복원된 해석 (`content`=Agent 원본), `status`, `revision`, `updatedAt`
            """
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @PatchMapping("/{scenarioGroupId}/scenarios/{scenarioId}/price-interpretation/reset")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PriceInterpretationRes> resetPriceInterpretation(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Parameter(description = "시나리오 ID", example = "11") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(priceInterpretationService.resetInterpretation(scenarioGroupId, scenarioId));
    }

}
