package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamProposalRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
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
import org.springframework.http.MediaType;
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
            변경 항목이 없어 거절되면 실패를 저장한 뒤 기존 HTTP 500 / API_RESPONSE_ERROR를 반환합니다.
            POST 오류 후에도 같은 경로의 GET으로 현재 상태와 실패 사유를 확인하세요.
            변경 항목 없음 거절에는 callback/SSE가 없으며, 입력 조건을 바꾼 뒤 새 요청을 보낼 수 있습니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "접수 성공")
    @ApiResponse(responseCode = "409", description = "RSC_ALREADY_EXISTS: 이미 진행 중인 제안 요청 또는 모델 실행이 존재함",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class)))
    @ApiResponse(responseCode = "500", description = "API_RESPONSE_ERROR: Agent HTTP 오류. 변경 항목 없음 실패도 포함하며 현재 상태는 GET으로 확인",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(name = "API_RESPONSE_ERROR", value = """
                {"status":false,"code":"API-004","message":"Received an error response from external API.","data":null}
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

    @ApiOrder(2)
    @Operation(
        summary = "수급모델 파라미터 현재 상태 조회 API",
        description = """
            팝업 진입 또는 생성 POST 오류 후 시나리오의 최신 요청 상태를 조회합니다.
            PROPOSAL_FAILED와 PROPOSAL_NO_CHANGES는 모두 제안 생성 실패이며, 후자는 변경 항목 없음 사유를 구별합니다.
            두 실패에서는 로딩과 반복 조회를 종료하고 failureReason을 표시하세요.
            parameterSource=NONE, modelFiles=[]이므로 이전 제안 표를 비우고 해당 요청의 확정/실행을 막아야 합니다.
            Agent ID 발급 전 실패이면 modelParamProposalId가 null일 수 있으며, executionInputFiles도 null입니다.
            HTTP 200은 상태 조회 성공을 뜻합니다. GET 자체의 오류나 GENERATING은 제안 생성 실패와 구별하세요.
            """
    )
    @ApiResponse(responseCode = "200", description = "현재 상태 조회 성공. 제안 생성 실패 상태도 포함", useReturnTypeSchema = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples = @ExampleObject(name = "PROPOSAL_NO_CHANGES", summary = "Agent ID 발급 전 변경 항목 없음으로 생성 실패", value = """
                {
                  "status":true,"code":"SUC-000","message":"Success.",
                  "data":{
                    "scenarioId":309,"currentState":"PROPOSAL_NO_CHANGES","parameterSource":"NONE",
                    "execPrepId":51001,"modelParamProposalId":null,"scenarioGroupId":null,
                    "caseId":null,"runId":null,"modelExecutionStatus":null,"executionInputFiles":null,
                    "modelFiles":[],"failureReason":"파라미터로 반영할 변경 항목이 없습니다.",
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

    @ApiOrder(3)
    @Operation(
        summary = "수급모델 파라미터 제안 상태/결과 조회 API",
        description = "Backend execution-preparation row와 Agent proposal table을 조합해 상태와 제안 결과를 조회합니다."
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

    @ApiOrder(4)
    @Operation(
        summary = "수급모델 파라미터 제안 SSE 구독 API",
        description = "Agent proposal 완료/실패 신호를 scenario 단위 SSE로 구독합니다. 이벤트명은 `model-parameter-proposal`입니다."
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
