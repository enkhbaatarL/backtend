package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamProposalRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalCreateFailureRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
import com.sk.domain.mi.exception.ModelParamProposalCreateRejectedException;
import com.sk.domain.mi.service.nextgen.ModelParamProposalService;
import com.sk.domain.mi.service.nextgen.sse.registry.ModelParamProposalSseRegistry;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Profile("!dedicated")
@Tag(name = "05-2. Next Gen MI Model Parameter Proposal API", description = "Next Gen MI 수급모델 파라미터 제안 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenarios/{scenarioId}")
@RequiredArgsConstructor
public class ModelParamProposalController {

    private final ModelParamProposalService modelParamProposalService;
    private final ModelParamProposalSseRegistry sseRegistry;

    @ApiOrder(1)
    @Operation(
        summary = "수급모델 파라미터 제안 생성 접수 API",
        description = """
            시나리오의 파라미터 제안 생성을 요청합니다. 접수 성공은 생성 완료를 뜻하지 않습니다.
            Agent 응답의 status=error이면 오류 코드와 무관하게 일반 실패를 저장하고 HTTP 500 / API_RESPONSE_ERROR와
            data.failureReason을 반환합니다. 이 안전한 사유를 즉시 표시하고 같은 경로의 GET으로 상태를 동기화하세요.
            즉시 -202 거절에는 callback/SSE가 없습니다. status 누락/알 수 없는 값, 응답 형식 오류, timeout은 실패를
            확정하지 않고 기존 요청 상태와 오류 응답의 data=null을 유지합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "접수 성공")
    @ApiResponse(responseCode = "409", description = "RSC_ALREADY_EXISTS: 이미 진행 중인 제안 요청 또는 모델 실행이 존재함",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class)))
    @ApiResponse(responseCode = "500", description = "API_RESPONSE_ERROR: 명시적 status=error는 data.failureReason을 즉시 표시하고 GET으로 상태 동기화",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(name = "API_RESPONSE_ERROR", value = """
                {"status":false,"code":"API-004","message":"Received an error response from external API.","data":{"failureReason":"파라미터로 반영할 변경 항목이 없습니다."}}
                """)))
    @PostMapping(value = "/model-param-proposals", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelParamProposalAcceptRes> createModelParamProposal(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(modelParamProposalService.createProposal(scenarioId));
    }

    @ExceptionHandler(ModelParamProposalCreateRejectedException.class)
    private ResponseEntity<BaseResponse<ModelParamProposalCreateFailureRes>> handleCreateRejected(
        ModelParamProposalCreateRejectedException ex
    ) {
        ModelParamProposalCreateFailureRes data = new ModelParamProposalCreateFailureRes(ex.getFailureReason());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponseFactory.create(ResultCode.Error.API_RESPONSE_ERROR, data));
    }

    @Deprecated
    @ApiOrder(2)
    @Operation(
        summary = "수급모델 파라미터 현재 상태 조회 API (deprecated)",
        deprecated = true,
        description = """
            deprecated - 시나리오 파라미터 비교 팝업 전환 후 그룹 현황 일괄 조회 API(GET /scenario-groups/{scenarioGroupId}/model-param-proposals)를 사용하세요.
            구 단건 팝업 호환을 위해 유지되며 FE 전환 완료 후 제거 예정입니다.

            팝업 진입 또는 생성 POST 오류 후 시나리오의 최신 요청 상태를 조회합니다.
            새 생성 오류는 PROPOSAL_FAILED입니다. PROPOSAL_NO_CHANGES는 과거 저장 상태의 읽기 호환용입니다.
            PROPOSAL_RETRYABLE은 통신 오류로 중단된 상태로, 재시도 버튼을 노출하고 생성 POST를 다시 호출하면 됩니다.
            ABSTAINED는 보류(값 생성됐으나 유효 범위 이탈)로, 처리 방식은 실패와 동일하며 표기와 failureReason(보류 사유)만 구분합니다.
            실패·보류에서는 로딩과 반복 조회를 종료하고 failureReason을 표시하세요.
            parameterSource=NONE, modelFiles=[]이므로 이전 제안 표를 비우고 해당 요청의 확정/실행을 막아야 합니다.
            Agent ID 발급 전 실패이면 modelParamProposalId가 null일 수 있으며, executionInputFiles도 null입니다.
            HTTP 200은 상태 조회 성공을 뜻합니다. GET 자체의 오류나 GENERATING은 제안 생성 실패와 구별하세요.
            """
    )
    @ApiResponse(responseCode = "200", description = "현재 상태 조회 성공. 제안 생성 실패 상태도 포함", useReturnTypeSchema = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples = @ExampleObject(name = "PROPOSAL_FAILED", summary = "Agent ID 발급 전 명시적 오류 응답으로 생성 실패", value = """
                {
                  "status":true,"code":"SUC-000","message":"Success.",
                  "data":{
                    "scenarioId":309,"currentState":"PROPOSAL_FAILED","parameterSource":"NONE",
                    "execPrepId":51001,"modelParamProposalId":null,"scenarioGroupId":null,
                    "caseId":null,"runId":null,"modelExecutionStatus":null,"executionInputFiles":null,
                    "modelFiles":[],"failureReason":"파라미터 제안 생성 요청에 실패했습니다.",
                    "requestedAt":"2026-08-27T13:15:33","completedAt":null,"confirmedAt":null
                  }
                }
                """)))
    @GetMapping(value = "/model-param-proposals", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<CurrentModelParamProposalRes> getCurrentModelParamProposal(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(modelParamProposalService.getCurrentProposal(scenarioId));
    }

    @Deprecated
    @ApiOrder(3)
    @Operation(
        summary = "수급모델 파라미터 제안 상태/결과 조회 API (deprecated)",
        deprecated = true,
        description = """
            deprecated - 시나리오 파라미터 비교 팝업 전환 후 그룹 비교 그리드 조회 API(GET /scenario-groups/{scenarioGroupId}/model-param-proposals/compare)를 사용하세요.
            구 단건 팝업 호환을 위해 유지되며 FE 전환 완료 후 제거 예정입니다.

            Backend execution-preparation row와 Agent proposal table을 조합해 상태와 제안 결과를 조회합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-param-proposals/{modelParamProposalId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelParamProposalRes> getModelParamProposal(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId,
        @Parameter(description = "Agent model parameter proposal ID", example = "10001") @PathVariable Long modelParamProposalId
    ) {
        return BaseResponseFactory.create(modelParamProposalService.getProposal(scenarioId, modelParamProposalId));
    }

    @Deprecated
    @ApiOrder(4)
    @Operation(
        summary = "수급모델 파라미터 제안 SSE 구독 API (deprecated)",
        deprecated = true,
        description = """
            deprecated - 시나리오 파라미터 비교 팝업 전환 후 그룹 SSE 구독 API(GET /scenario-groups/{scenarioGroupId}/model-param-proposals/stream)를 사용하세요.
            구 단건 팝업 호환을 위해 유지되며 FE 전환 완료 후 제거 예정입니다.

            Agent proposal 완료/실패 신호를 scenario 단위 SSE로 구독합니다. 이벤트명은 `model-parameter-proposal`입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream)")
    @GetMapping(value = "/model-param-proposals/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter streamModelParamProposal(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return sseRegistry.connect(scenarioId);
    }
}
