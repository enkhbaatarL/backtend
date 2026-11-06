package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.EventVerificationReq;
import com.sk.domain.mi.dto.response.nextgen.EventVerificationRes;
import com.sk.domain.mi.service.nextgen.EventVerificationService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@RestController
@RequestMapping("/api/v1/mi/next-gen/events")
@Tag(name = "01-1. Next Gen MI Event API")
@RequiredArgsConstructor
public class EventVerificationController {
    private final EventVerificationService service;

    @ApiOrder(8)
    @Operation(summary = "Event 웹 근거 검증", description = "게시·미삭제 Event 1~5개를 웹 근거와 비교합니다. "
        + "data.items는 요청 ID 순서이며 HTTP 200도 warning/fail을 포함합니다. "
        + "checkedAt은 검증 기준 시각입니다. 이력 저장 및 시나리오 생성 차단은 수행하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "Event별 판정 반환 (일부 검색·비교 실패도 warning 포함)")
    @ApiResponse(responseCode = "401", description = "로그인 필요")
    @ApiResponse(responseCode = "403", description = "Event 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "요청 ID 중 하나라도 없음/삭제/미게시")
    @ApiResponse(responseCode = "422", description = "잘못된 형식·ID·개수·중복·추가 필드")
    @ApiResponse(responseCode = "500", description = "요청 전체 처리 실패")
    @ApiResponse(responseCode = "502", description = "Agent 연결 실패 또는 잘못된 응답")
    @ApiResponse(responseCode = "503", description = "Agent 서비스 사용 불가")
    @ApiResponse(responseCode = "504", description = "대기시간 초과")
    @PostMapping("/verifications")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventVerificationRes> verify(@Valid @RequestBody EventVerificationReq request) {
        return BaseResponseFactory.create(service.verify(request));
    }
}
