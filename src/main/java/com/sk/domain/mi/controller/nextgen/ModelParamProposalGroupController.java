package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamProposalsRes;
import com.sk.domain.mi.service.nextgen.ModelParamProposalGroupService;
import com.sk.domain.mi.service.nextgen.sse.registry.GroupModelParamProposalSseRegistry;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Profile("!dedicated")
@Tag(name = "05-3. Next Gen MI Scenario Parameter Comparison API", description = "Next Gen MI 시나리오 파라미터 비교(그룹 단위) API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}")
@RequiredArgsConstructor
public class ModelParamProposalGroupController {

    private final ModelParamProposalGroupService modelParamProposalGroupService;
    private final GroupModelParamProposalSseRegistry groupSseRegistry;

    @ApiOrder(1)
    @Operation(
        summary = "그룹 파라미터 제안 SSE 구독 API",
        description = """
            시나리오 파라미터 비교 팝업에서 그룹 내 시나리오들의 추천 파라미터 **생성 완료/실패 신호**를 그룹 단위 연결 하나로 구독합니다. <br>
            팝업 진입 시 1회 연결하며, 시나리오별로 스트림을 따로 열 필요가 없습니다.
            (생성 트리거는 시나리오 단건 [파라미터 제안 생성 접수 API]의 POST입니다.)

            <br><br>
            ### 전송 형식
            `text/event-stream`. 각 프레임은 `event:`(이벤트 종류)와 `data:`(JSON)로 구성됩니다. <br>
            진행 신호가 없는 동안 keep-alive 주석 프레임(`: keep-alive`)이 약 15초 간격으로 전달됩니다.
            (event/data가 없는 주석 프레임이므로 연결 생존 신호로만 취급(무시)합니다.) <br>
            연결 타임아웃은 30분이며, 타임아웃/절단 시 같은 URL로 다시 연결하면 됩니다.

            <br><br>
            ### 이벤트 종류 & 처리 가이드
            **(1) `connected`** — 연결 직후 1회 전달되는 연결 확인 신호. 별도 처리 없이 무시해도 됩니다.
            ```
            event: connected
            data: {"scenarioGroupId":61}
            ```

            **(2) `model-parameter-proposal`** — 그룹 내 어느 시나리오의 제안 생성이 끝났다는 신호.
            `scenarioId`로 갱신할 팝업 행을 판별하고, `status`로 결과를 분기합니다.
            - `status=PROPOSED`: 생성 완료 — 해당 시나리오 상태를 완료로 바꾸고, [그룹 파라미터 비교 그리드 조회 API]를 재조회해 열을 채웁니다.
            - `status=FAILED`: 생성 실패 — 실패 표기로 바꾸고 실패 사유는 [그룹 파라미터 제안 현황 일괄 조회 API]로 확인합니다.
            ```
            event: model-parameter-proposal
            data: {"scenarioId":309,"scenarioGroupId":61,"modelParamProposalId":10001,"status":"PROPOSED"}
            ```
            실패인데 Agent ID 발급 전이면 `modelParamProposalId` 필드가 생략됩니다 (null 필드 미직렬화).
            ```
            event: model-parameter-proposal
            data: {"scenarioId":310,"scenarioGroupId":61,"status":"FAILED"}
            ```
            이벤트명은 시나리오 단건 스트림과 동일하므로 FE는 한 규칙으로 payload를 처리하면 됩니다.

            <br><br>
            ### 신호 유실 대비 (SSE는 신호, 정본은 GET)
            상태의 정본은 [그룹 파라미터 제안 현황 일괄 조회 API]입니다. <br>
            재연결 직후에는 끊긴 사이 신호가 유실됐을 수 있으므로 현황 조회를 1회 재호출하세요.
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공 (text/event-stream)")
    @GetMapping(value = "/model-param-proposals/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public SseEmitter streamGroupModelParamProposal(
        @Parameter(description = "시나리오 그룹 ID", example = "61") @PathVariable Long scenarioGroupId
    ) {
        return groupSseRegistry.connect(scenarioGroupId);
    }

    @ApiOrder(2)
    @Operation(
        summary = "그룹 파라미터 제안 현황 일괄 조회 API",
        description = """
            수급모델 실행 팝업 진입 시 그룹 내 시나리오 전체의 제안 상태를 한 번에 조회합니다.
            시나리오별 currentState는 단건 현재 상태 조회 API와 동일한 규칙이며, 팝업 상태 컬럼(5종)은 아래 매핑으로 표시합니다.

            ### currentState → 상태 컬럼 매핑
            - NOT_REQUESTED → 생성 ([생성] 버튼)
            - GENERATING → 생성 중 (스피너)
            - PROPOSAL_READY / CONFIRMED_PENDING_EXECUTION / EXECUTING / EXECUTED_CONFIRMED / EXECUTION_FAILED → 완료 (실행 실패도 확정 파라미터가 존재해 그리드 노출·선택·재실행이 가능하므로 완료 그룹. EXECUTING은 그리드 값 영역 스켈레톤 처리)
            - PROPOSAL_FAILED / PROPOSAL_NO_CHANGES → 실패 (failureReason 툴팁, 체크 불가)
            - PROPOSAL_RETRYABLE → 재시도 (통신 오류 중단 — 시나리오 단건 생성 POST 재호출로 재시도)

            분기값·차원 헤더는 그룹 상세 API를 사용하세요.
            실행가능성 컬럼은 adjustedProbability(폐기 제외 비례 재분배, 폐기=0)를 표시하세요 — probability는 폐기해도 유지되는 원본입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping(value = "/model-param-proposals", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<GroupModelParamProposalsRes> getGroupModelParamProposals(
        @Parameter(description = "시나리오 그룹 ID", example = "61") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(modelParamProposalGroupService.getGroupProposals(scenarioGroupId));
    }

    @ApiOrder(3)
    @Operation(
        summary = "그룹 파라미터 비교 그리드 조회 API",
        description = """
            완료(제안 보유 — currentState가 PROPOSAL_READY/CONFIRMED_PENDING_EXECUTION/EXECUTING/EXECUTED_CONFIRMED/EXECUTION_FAILED) 시나리오들의 파라미터를 slot 축 기준으로 행 병합한 비교 그리드를 조회합니다.
            미제안 셀은 null이므로 화면에서 "-" 또는 공백으로 표시하세요. basecaseValue(MI Base)는 행별로 처음 발견된 non-null 기준값이며 없으면 null입니다.
            modelTarget/parameterShapes/modelFiles는 단건 제안 조회와 같은 구조·위치입니다. 완료 시나리오가 없으면 scenarios/parameters가 빈 배열입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping(value = "/model-param-proposals/compare", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<GroupModelParamCompareRes> getGroupModelParamCompare(
        @Parameter(description = "시나리오 그룹 ID", example = "61") @PathVariable Long scenarioGroupId
    ) {
        return BaseResponseFactory.create(modelParamProposalGroupService.getGroupCompare(scenarioGroupId));
    }

}
