package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssuePreviewRes;
import com.sk.domain.mi.service.newsletter.NewsletterSnapshotService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "13-4. Newsletter Snapshot API", description = "뉴스레터 snapshot API입니다.")
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterSnapshotController {

    private final NewsletterSnapshotService newsletterSnapshotService;

    @ApiOrder(15)
    @Operation(summary = "뉴스레터 snapshot 조회", description = "템플릿 기본 구조 기반으로 snapshot을 동적으로 조립합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/{issueNo}/snapshot")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterIssuePreviewRes> generateSnapshot(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            newsletterSnapshotService.generateSnapshot(newsletterId, issueNo, actorUser)
        );
    }
}
