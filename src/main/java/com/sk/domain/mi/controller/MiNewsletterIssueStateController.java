package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.newsletter.NewsletterIssueStateSaveReq;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssuePreviewRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueStateSaveRes;
import com.sk.domain.mi.service.newsletter.NewsletterIssuePreviewWriteService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "13-4. Newsletter Issue State API", description = "뉴스레터 issue state preview/write API입니다.")
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterIssueStateController {

    private final NewsletterIssuePreviewWriteService newsletterIssuePreviewWriteService;

    @ApiOrder(15)
    @Operation(summary = "뉴스레터 preview 조회", description = "issue state row를 기반으로 preview snapshot을 동적으로 조립합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/{issueNo}/preview")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterIssuePreviewRes> getPreview(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            newsletterIssuePreviewWriteService.getPreview(newsletterId, issueNo, actorUser)
        );
    }

    @ApiOrder(16)
    @Operation(summary = "뉴스레터 issue state 저장", description = "section/block state row를 upsert하고 repeated block row를 blk_ord 순으로 저장합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/{newsletterId}/{issueNo}/issue-state")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterIssueStateSaveRes> saveIssueState(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @RequestBody NewsletterIssueStateSaveReq request,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            newsletterIssuePreviewWriteService.saveIssueState(newsletterId, issueNo, request, actorUser)
        );
    }
}
