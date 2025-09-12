package com.sk.domain.mi.controller;

import com.sk.domain.mi.dto.response.newsletter.NewsletterFeaturedListRes;
import com.sk.domain.shared.enums.PermissionCondition;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterListRes;
import com.sk.domain.mi.service.MiNewsletterService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@Tag(name = "13. Newsletter API", description = "뉴스레터 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterController {

    private final AuthorizationService authorizationService;
    private final MiNewsletterService miNewsletterService;

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "page", description = "페이지번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당항목수 (기본값: 20)"),
            @Parameter(name = "keyword", description = "검색키워드 (제목, 이름 검색)")
    })
    @Operation(summary = "뉴스레터 목록 조회", description = "뉴스레터 목록을 조회합니다. (현재 Mock 데이터)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<NewsletterListRes>> getNewsletterList(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        return BaseResponseFactory.create(
            miNewsletterService.getNewsletterList(page, size, keyword)
        );
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "page", description = "페이지번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당항목수 (기본값: 6)")
    })
    @Operation(summary = "뉴스레터 featured 조회", description = "각 뉴스레터별로 가장 최근 발행된 호를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/featured")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<NewsletterFeaturedListRes>> getLatestNewsletters(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "6") @Min(value = 1) int size) {
        return BaseResponseFactory.create(miNewsletterService.getLatestNewsletters(page, size));
    }
}
