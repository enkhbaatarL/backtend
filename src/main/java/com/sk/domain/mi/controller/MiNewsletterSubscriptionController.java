package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.newsletter.NewsletterSubscriptionRes;
import com.sk.domain.mi.service.MiNewsletterSubscriptionService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "13-2. Newsletter Subscription API", description = "뉴스레터 구독 API입니다.")
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterSubscriptionController {

    private final MiNewsletterSubscriptionService miNewsletterSubscriptionService;

    @ApiOrder(5)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Operation(summary = "뉴스레터 구독", description = "현재 사용자를 뉴스레터 구독 상태로 변경합니다. 멱등하게 동작합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/{newsletterId}/subscription")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterSubscriptionRes> subscribe(
        @PathVariable Integer newsletterId,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterSubscriptionService.subscribe(newsletterId, actorUser)
        );
    }

    @ApiOrder(6)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Operation(summary = "뉴스레터 구독 해지", description = "현재 사용자를 뉴스레터 미구독 상태로 변경합니다. 멱등하게 동작합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/{newsletterId}/subscription")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterSubscriptionRes> unsubscribe(
        @PathVariable Integer newsletterId,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterSubscriptionService.unsubscribe(newsletterId, actorUser)
        );
    }

    @ApiOrder(7)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Operation(summary = "내 뉴스레터 구독 상태 조회", description = "현재 사용자의 뉴스레터 구독 상태를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{newsletterId}/subscription")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterSubscriptionRes> getSubscriptionStatus(
        @PathVariable Integer newsletterId,
        @UserResolve User actorUser
    ) {
        return BaseResponseFactory.create(
            miNewsletterSubscriptionService.getStatus(newsletterId, actorUser)
        );
    }
}
