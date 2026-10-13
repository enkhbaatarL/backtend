package com.sk.domain.mi.controller.nextgen.mcp;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.request.common.McpUserFeedbackReq;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.CommonService;
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

/**
 * [MCP Feedback] 사용자 피드백 등록 — MCP 채널의 쓰기 API (읽기 전용 채널의 예외. 이벤트 생성과 함께 2건).
 * 원본 화면 API(POST /api/v1/common/user-feedback)는 일반 서버 전용이라 전용 서버에 MCP 경로로 둔다.
 * 저장은 화면과 같은 tcm_usrfedbl 이며 채널 코드(fedb_chnl_cd)만 MCP 로 다르다.
 */
@Profile("dedicated")
@Tag(name = "MCP Next Gen MI User Feedback API", description = "MCP client용 사용자 피드백 등록 API입니다.")
@RestController
@RequestMapping("/api/v1/mcp/mi/next-gen")
@RequiredArgsConstructor
public class McpUserFeedbackDedicatedController {

    private final CommonService commonService;

    @ApiOrder(21)
    @Operation(
        summary = "사용자 피드백 등록",
        description = """
            사용자가 Next Gen MI 정보나 AI 응답에 대해 남기는 피드백을 1건 등록합니다. 사용자가 피드백 등록을 명시적으로 요청한 경우에만 호출하세요. \
            화면의 피드백 버튼과 같은 곳에 저장되며 운영자가 확인합니다. 피드백 내용만 필수이며, 유형·대상 종류·대상 ID 가 정확하지 않아도 등록됩니다. \
            특정 이벤트·시나리오 그룹·시나리오에 대한 피드백이면 feedbackSectionNm 과 targetId(조회 결과의 ID), feedbackPageNm(대상 이름)을 함께 넣습니다. \
            등록만 가능하고 조회·수정·삭제는 지원하지 않으며, 다시 호출하면 중복 등록됩니다. data 는 등록된 피드백 번호입니다.""")
    @ApiResponse(responseCode = "200", description = "등록 성공. data 는 등록된 피드백 번호")
    @PostMapping("/user-feedback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY}),
        @PermissionRule(role = Role.OPERATOR) // [MCP Operator] 회사는 인터셉터의 MCP 키 회사 일치 검사로 제한
    })
    public BaseResponse<Long> createMcpUserFeedback(@UserResolve User user, @Valid @RequestBody McpUserFeedbackReq request) {
        return BaseResponseFactory.create(commonService.createMcpUserFeedback(user, request));
    }

}
