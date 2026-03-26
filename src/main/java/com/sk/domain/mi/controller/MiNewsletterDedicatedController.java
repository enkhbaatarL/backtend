package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailV2Res;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueOriginalSnapshotRes;
import com.sk.domain.mi.service.MiNewsletterService;
import com.sk.domain.mi.service.newsletter.NewsletterDetailV2Service;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "13-1. Newsletter API", description = "뉴스레터 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterDedicatedController {

    private final MiNewsletterService miNewsletterService;
    private final NewsletterDetailV2Service newsletterDetailV2Service;
    private final AuthorizationService authorizationService;

    @ApiOrder(4)
    @Parameter(name = "companyNo", description = "고객회사번호")
    @Operation(summary = "뉴스레터 상세 조회", description = "특정 뉴스레터의 특정 발행호 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping(value = "/{newsletterId}/{issueNo}/old", params = "companyNo")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterDetailRes> getNewsletterDetail(
        @PathVariable String newsletterId,
        @PathVariable String issueNo,
        @RequestParam(value = "companyNo") Integer companyNo
        ) {
        Integer userNo = authorizationService.getUserNo();

        return BaseResponseFactory.create(
            miNewsletterService.getNewsletterDetail(companyNo, userNo, newsletterId, issueNo)
        );
    }

    @ApiOrder(5)
    @Parameter(name = "companyNo", description = "접근 제어와 tenant resolution에 사용하는 요청 고객회사번호")
    @Operation(
        summary = "뉴스레터 상세 v2 조회",
        description = "요청 companyNo로 접근 가능한 발행본을 먼저 확인한 뒤, actor user 소유 수정본이 있으면 수정본을, 없으면 원본을 NewsletterDetailV2Res로 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping(value = "/{newsletterId}/{issueNo}", params = "companyNo")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterDetailV2Res> getNewsletterDetailV2(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @RequestParam(value = "companyNo") Integer companyNo
    ) {
        Integer userNo = authorizationService.getUserNo();
        return BaseResponseFactory.create(
            newsletterDetailV2Service.getNewsletterDetail(companyNo, userNo, newsletterId, issueNo)
        );
    }

    @ApiOrder(6)
    @Parameter(name = "companyNo", description = "접근 제어와 tenant resolution에 사용하는 요청 고객회사번호")
    @Operation(summary = "뉴스레터 original snapshot 조회", description = "요청 companyNo 기준으로 접근 가능한 published issue의 org_snpsht_json 원본 스냅샷만 그대로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping(value = "/{newsletterId}/{issueNo}/original-snapshot", params = "companyNo")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsletterIssueOriginalSnapshotRes> getNewsletterOriginalSnapshot(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @RequestParam(value = "companyNo") Integer companyNo
    ) {
        return BaseResponseFactory.create(
            newsletterDetailV2Service.getOriginalSnapshot(companyNo, newsletterId, issueNo)
        );
    }
}
