package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.EventVerificationUpdateReq;
import com.sk.domain.mi.dto.response.nextgen.EventVerificationUpdateRes;
import com.sk.domain.mi.service.nextgen.EventVerificationUpdateService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@RestController
@RequestMapping("/api/v1/mi/next-gen/events")
@Tag(name = "01-1. Next Gen MI Event API")
@RequiredArgsConstructor
public class EventVerificationUpdateController {
    private final EventVerificationUpdateService service;

    @ApiOrder(9)
    @Operation(summary = "Event 웹 검증 기반 업데이트", description = "Agent가 새 웹 검증·수정안 재검증 후 변경 전 Snapshot과 Event를 저장합니다. "
        + "data.updateStatus=updated만 저장 성공이며 상세를 다시 조회합니다. pass는 unchanged, warning은 skipped입니다. "
        + "actorUsrLognId는 로그인 사용자로 설정합니다. 응답 유실·시간 초과 시 자동 재시도하지 말고 Event/Snapshot을 확인하세요.")
    @ApiResponse(responseCode = "200", description = "updated / unchanged / skipped 처리 결과")
    @ApiResponse(responseCode = "401", description = "로그인 필요")
    @ApiResponse(responseCode = "403", description = "Event 수정 권한 없음")
    @ApiResponse(responseCode = "404", description = "Event 없음·삭제·미게시")
    @ApiResponse(responseCode = "409", description = "EVENT_UPDATE_STALE: 최신 Event 재조회 필요 (BIZ-007)")
    @ApiResponse(responseCode = "422", description = "양의 Event ID와 선택적 expectedUpdatedAt만 허용")
    @ApiResponse(responseCode = "500", description = "Agent 처리 또는 Event/Snapshot 스키마 오류")
    @ApiResponse(responseCode = "502", description = "Agent 연결 실패 또는 잘못된 응답. 저장 여부 확인 필요")
    @ApiResponse(responseCode = "503", description = "Agent 서비스 사용 불가")
    @ApiResponse(responseCode = "504", description = "시간 초과. 재시도 전 저장 여부 확인 필요")
    @PostMapping("/{miEventId}/verification-updates")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<EventVerificationUpdateRes> update(@PathVariable @Positive Long miEventId,
                                                          @Valid @RequestBody EventVerificationUpdateReq request) {
        return BaseResponseFactory.create(service.update(miEventId, request));
    }
}
