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
    @Operation(summary = "Event 웹 검증 기반 업데이트", description = "앞선 검증의 verificationId가 필수입니다. Agent가 저장된 근거로 수정안을 생성·재검증한 뒤 Snapshot과 Event를 저장하며 새 웹 검색은 하지 않습니다. "
        + "저장 여부는 data.updateStatus로 판단하며 updated일 때 상세를 다시 조회합니다. 공개 검증 pass와 skipped도 함께 반환될 수 있습니다. "
        + "no_contradiction은 명확한 불일치 없음이며 checks의 주의·미확인 사항을 함께 표시합니다. "
        + "actorUsrLognId는 로그인 사용자로 설정합니다. 응답 유실·시간 초과 시 자동 재시도하지 말고 Event/Snapshot을 확인하세요.")
    @ApiResponse(responseCode = "200", description = "updated / unchanged / skipped 처리 결과")
    @ApiResponse(responseCode = "401", description = "로그인 필요")
    @ApiResponse(responseCode = "403", description = "Event 수정 권한 없음")
    @ApiResponse(responseCode = "404", description = "Event 없음·삭제·미게시 또는 해당 Event의 검증 ID 없음")
    @ApiResponse(responseCode = "409", description = "Event 변경(BIZ-007), 검증 만료(BIZ-008), 이미 적용(BIZ-009). data.error_name 및 기존 Snapshot ID 확인")
    @ApiResponse(responseCode = "422", description = "양의 Event ID, 필수 verificationId UUID와 선택적 expectedUpdatedAt만 허용")
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
