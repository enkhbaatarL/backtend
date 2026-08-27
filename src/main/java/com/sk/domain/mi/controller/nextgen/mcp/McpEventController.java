package com.sk.domain.mi.controller.nextgen.mcp;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventLinkedScenarioGroupRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "MCP Next Gen MI Event API", description = "MCP client용 Next Gen MI 이벤트 조회 API입니다.")
@RestController
@RequestMapping("/api/v1/mcp/mi/next-gen/events")
@RequiredArgsConstructor
public class McpEventController {

    private final EventService eventService;

    @ApiOrder(1)
    @Operation(summary = "이벤트 유형 L1 목록 조회", description = "이벤트 목록 필터에 사용할 이벤트 유형 Level 1 마스터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/event-types/lv1")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<List<EventTypeL1Res>> getEventTypesL1() {
        return BaseResponseFactory.create(eventService.getEventTypesL1());
    }

    @ApiOrder(2)
    @Operation(summary = "이벤트 목록 조회", description = "필터, 정렬, 페이징 조건으로 Next Gen MI 이벤트 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventListRes> getEvents(@Valid @ParameterObject EventListReq request) {
        return BaseResponseFactory.create(eventService.getEvents(request));
    }

    @ApiOrder(3)
    @Operation(
        summary = "이벤트 연결 시나리오 그룹 목록 조회",
        description = "이벤트에 연결된 시나리오 그룹을 조회합니다. `mine`은 `X-User-Id`가 지정한 effective user 기준입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공. 연결된 그룹이 없으면 빈 목록")
    @GetMapping("/{eventId}/scenario-groups")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<List<EventLinkedScenarioGroupRes>> getEventScenarioGroups(
        @Parameter(description = "이벤트 ID", example = "1523") @PathVariable Long eventId
    ) {
        return BaseResponseFactory.create(eventService.getEventScenarioGroups(eventId));
    }

    @ApiOrder(4)
    @Operation(summary = "이벤트 상세 조회", description = "이벤트 상세와 영향 분석, AI 인사이트, 연결 시나리오 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventDetailRes> getEventDetail(
        @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long eventId
    ) {
        return BaseResponseFactory.create(eventService.getEventDetail(eventId));
    }

    @ApiOrder(5)
    @Operation(summary = "이벤트 기관 전망 조회", description = "이벤트와 관련된 기관별 시장 전망을 전망 일자 내림차순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/outlooks")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventInstitutionOutlookRes>> getEventOutlooks(
        @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long eventId,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventService.getEventOutlooks(eventId, page, size));
    }

    @ApiOrder(6)
    @Operation(
        summary = "이벤트 연관 레코드 조회",
        description = "이벤트 연관 뉴스·리포트를 조회합니다. `isPermitted`는 effective user와 서버가 결정한 SKGAS 상품 권한 기준입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{eventId}/records")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<PaginationRes<EventRelatedRecordRes>> getEventRecords(
        @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long eventId,
        @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
        @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return BaseResponseFactory.create(eventService.getEventRecords(eventId, page, size));
    }
}
