package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.newsletter.RevisionSaveReq;
import com.sk.domain.mi.dto.response.newsletter.RevisionDraftRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionFlowGuideRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionListRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionReplayRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionSaveRes;
import com.sk.domain.mi.service.MiNewsletterRevisionService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "13-3. Newsletter Revision API", description = "뉴스레터 리비전 API입니다.")
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterRevisionController {

    private final MiNewsletterRevisionService miNewsletterRevisionService;

    @ApiOrder(8)
    @Operation(summary = "리비전 저장/조회 플로우 요약")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/{issueNo}/revisions/flow")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionFlowGuideRes> getRevisionFlowGuide(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.getRevisionFlowGuide(newsletterId, issueNo)
        );
    }

    @ApiOrder(10)
    @Operation(summary = "수정 이력 목록 조회")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/{issueNo}/revisions/{revisionId}/changes")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionListRes> getRevisionChanges(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @PathVariable Long revisionId,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.getRevisionChanges(newsletterId, issueNo, revisionId, actorUser)
        );
    }

    @ApiOrder(11)
    @Operation(summary = "특정 수정 이력 조회")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/{issueNo}/revisions/{revisionId}/changes/{seq}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionReplayRes> getRevisionChangeBySeq(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @PathVariable Long revisionId,
        @PathVariable Long seq,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.getRevisionChangeBySeq(newsletterId, issueNo, revisionId, seq, actorUser)
        );
    }

    @ApiOrder(12)
    @Operation(summary = "임시저장 최신본 조회")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/{issueNo}/revisions/draft")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionDraftRes> getDraft(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.getDraft(newsletterId, issueNo, actorUser)
        );
    }

    @ApiOrder(13)
    @Operation(summary = "임시저장")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/{newsletterId}/{issueNo}/revisions/draft")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionSaveRes> saveDraft(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @RequestBody(required = false) RevisionSaveReq request,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.saveDraft(newsletterId, issueNo, request, actorUser)
        );
    }

    @ApiOrder(14)
    @Operation(summary = "최종저장")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/{newsletterId}/{issueNo}/revisions/save")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<RevisionSaveRes> saveFinal(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @RequestBody(required = false) RevisionSaveReq request,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterRevisionService.saveFinal(newsletterId, issueNo, request, actorUser)
        );
    }
}
