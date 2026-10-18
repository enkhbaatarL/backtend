package com.sk.domain.mi.controller.nextgen.mcp;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationJobRes;
import com.sk.domain.mi.service.nextgen.EventGenerationService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [MCP Event Generate] 이벤트 생성 비동기 작업 API (접수·작업 조회·작업 목록).
 * SK Gas 전용 서버(dedicated profile)에서만 쓴다 — 실행 스레드 풀·생성 서버 클라이언트가 dedicated 에서만 만들어지므로
 * 이벤트 조회 API(McpEventDedicatedController)와 분리해 이 컨트롤러만 dedicated 로 둔다. 경로·태그는 이벤트 API 와 같다.
 */
@Profile("dedicated")
@Tag(name = "MCP Next Gen MI Event API", description = "MCP client용 Next Gen MI 이벤트 조회·생성 API입니다.")
@RestController
@RequestMapping("/api/v1/mcp/mi/next-gen/events")
@RequiredArgsConstructor
public class McpEventGenerationDedicatedController {

    private final EventGenerationService eventGenerationService;

    /**
     * [MCP Event Generate] 이벤트 생성 접수 — B.E 는 인증(MCP 키 + X-User-Id) 후 작업만 만들고 즉시 응답한다.
     * 이벤트 생성 서버 호출(최대 15분)은 백그라운드에서 하며, 결과는 작업 조회(8)로 받는다. 설명 문구는 event_generate_v1 설명에 MCP 호출 조건을 덧붙인 것이다.
     */
    @ApiOrder(7)
    @Operation(
        summary = "사용자 입력 문장 기반 이벤트 생성 접수",
        description = """
            사용자 입력 문장을 분석해 웹 검색 또는 기존 이벤트 검색으로 근거를 수집하고, 그 근거로 이벤트를 생성해 저장하는 작업을 접수합니다. \
            사용자가 이벤트 생성을 명시적으로 요청한 경우에만 호출하세요. 생성에는 수분이 걸리므로 이 API 는 결과를 기다리지 않고 작업 ID(jobId)를 즉시 돌려줍니다. \
            결과는 jobId 로 이벤트 생성 작업 조회 API 에서 확인합니다. 사용자에게 접수됐다고 알리고, 사용자가 결과를 물으면 그때 조회하세요. \
            같은 사용자가 같은 입력으로 진행 중인 작업이 있으면 새로 만들지 않고 그 작업을 돌려줍니다(reused=true). \
            완료된 뒤 다시 호출하면 이벤트가 중복 생성됩니다. \
            동시에 실행할 수 있는 생성 작업 수에 한도가 있으며, 한도를 넘으면 BIZ-001 오류가 나므로 잠시 후 다시 요청하도록 안내하세요.""")
    @ApiResponse(responseCode = "200", description = "접수 성공. data 는 작업 ID 와 상태")
    @PostMapping("/generations")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY}),
        @PermissionRule(role = Role.OPERATOR) // [MCP Operator] 회사는 인터셉터의 MCP 키 회사 일치 검사로 제한
    })
    public BaseResponse<EventGenerationAcceptRes> generateEvents(@UserResolve User user, @Valid @RequestBody EventGenerationReq request) {
        return BaseResponseFactory.create(eventGenerationService.accept(user, request));
    }

    @ApiOrder(8)
    @Operation(
        summary = "이벤트 생성 작업 조회",
        description = """
            이벤트 생성 접수로 받은 jobId 의 상태와 결과를 조회합니다. `X-User-Id` 사용자 본인의 작업만 조회됩니다. \
            status 가 RUNNING 이면 아직 생성 중이므로 사용자에게 진행 중이라고 알리고, 짧은 간격으로 반복 조회하지 마세요. \
            SUCCEEDED 이면 result 에 생성 결과(meta, events)가 있고, 저장된 이벤트는 miEventId 를 eventId 로 이벤트 상세 조회에 쓸 수 있습니다. \
            FAILED 이면 errorCode, errorMessage 에 실패 사유가 있습니다. \
            INTERRUPTED 이면 서버 재기동 등으로 결과를 받지 못한 것이며 이벤트는 저장됐을 수 있으므로, 다시 생성하기 전에 이벤트 목록에서 확인하세요.""")
    @ApiResponse(responseCode = "200", description = "조회 성공. 작업이 없거나 다른 사용자의 작업이면 RSC-001")
    @GetMapping("/generations/{jobId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY}),
        @PermissionRule(role = Role.OPERATOR) // [MCP Operator] 회사는 인터셉터의 MCP 키 회사 일치 검사로 제한
    })
    public BaseResponse<EventGenerationJobRes> getEventGeneration(
        @UserResolve User user,
        @Parameter(description = "이벤트 생성 작업 ID(접수 응답의 jobId)", example = "42") @PathVariable Long jobId
    ) {
        return BaseResponseFactory.create(eventGenerationService.getJob(user, jobId));
    }

    @ApiOrder(9)
    @Operation(
        summary = "이벤트 생성 작업 목록 조회",
        description = """
            `X-User-Id` 사용자 본인의 이벤트 생성 작업을 최신순으로 조회합니다. jobId 를 모를 때 최근 작업을 찾는 데 씁니다. \
            목록에는 생성 결과(result)가 담기지 않으므로 결과는 이벤트 생성 작업 조회로 확인하세요.""")
    @ApiResponse(responseCode = "200", description = "조회 성공. 작업이 없으면 빈 목록")
    @GetMapping("/generations")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY}),
        @PermissionRule(role = Role.OPERATOR) // [MCP Operator] 회사는 인터셉터의 MCP 키 회사 일치 검사로 제한
    })
    public BaseResponse<PaginationRes<EventGenerationJobRes>> getEventGenerations(
        @UserResolve User user,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventGenerationService.getJobs(user, page, size));
    }

}
