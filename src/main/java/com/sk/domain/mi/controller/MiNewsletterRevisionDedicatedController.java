package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.newsletter.RevisionOrOriginalDetailRes;
import com.sk.domain.mi.service.MiNewsletterRevisionService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "13-3. Newsletter Revision API", description = "뉴스레터 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterRevisionDedicatedController {

    private final MiNewsletterRevisionService miNewsletterRevisionService;
    private final AuthorizationService authorizationService;

    @ApiOrder(9)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Parameter(name = "issueNo", description = "회차 번호")
    @Operation(summary = "원본 or 편집본 상세 조회")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping(value = "/{newsletterId}/{issueNo}/test", params = "!companyNo")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionOrOriginalDetailRes> getRevisionOrOriginalDetail(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.getRevisionOrOriginalDetail(newsletterId, issueNo, actorUser)
        );
    }
}
