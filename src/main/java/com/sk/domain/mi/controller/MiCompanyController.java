package com.sk.domain.mi.controller;

import com.sk.domain.mi.dto.response.MiCompanyDetailRes;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.mi.service.MiCompanyService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
@Profile("!dedicated")
@Tag(name = "00. Company API", description = "Company API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/company-management")
@RequiredArgsConstructor
public class MiCompanyController {

    private final MiCompanyService miCompanyService;
    private final AuthorizationService authorizationService;

    @ApiOrder(1)
    @GetMapping("/companies/all")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<MiCompanyRes>> getCompanyList(HttpServletRequest request) {
        return BaseResponseFactory.create(miCompanyService.getCompanyList(request.getServerName()));
    }

    @ApiOrder(2)
    @GetMapping("/companies/{companyNo}/detail")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<MiCompanyDetailRes> getCompanyDetail(@PathVariable("companyNo") int companyNo, HttpServletRequest request) {
        // [회사 상세 조회 제한] 일반 사용자와 운영자는 자기 회사(token claim) 상세만 조회할 수 있다
        Role role = authorizationService.getCurrentRole();
        if ((role == Role.USER || role == Role.OPERATOR)
            && !Integer.valueOf(companyNo).equals(authorizationService.getCompanyNo())) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "USER/OPERATOR can access only own company detail");
        }
        return BaseResponseFactory.create(miCompanyService.getCompanyDetail(companyNo, request.getServerName()));
    }
}
