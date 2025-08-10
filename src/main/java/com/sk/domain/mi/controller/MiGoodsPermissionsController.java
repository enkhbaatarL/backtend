package com.sk.domain.mi.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiGoodsPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "00. 상품권한 조회 API", description = "상품권한 조회 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/goods-info")
@RequiredArgsConstructor
public class MiGoodsPermissionsController {

    private final MiGoodsPermissionService goodsPermissionService;

    @Operation(summary = "사용자 상품권한 목록 조회 API", description = "사용자 번호를 통해 사용자의 상품권한 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/users/{userNo}/permissions")
    public BaseResponse<PermissionRes> getUserGoodsPermissions(@PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(goodsPermissionService.getUserGoodsPermissions(userNo));
    }

    @Operation(summary = "관리자 상품권한 목록 조회 API", description = "타사접속권한이 있는 관리자의 상품권한 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/admin/permissions")
    public BaseResponse<PermissionRes> getAccessAdminGoodsPermissions() {
        return BaseResponseFactory.create(goodsPermissionService.getAccessAdminGoodsPermissions());
    }

    @Operation(summary = "운영자 상품권한 목록 조회 API", description = "사용자 번호를 통해 사용자의 상품권한 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/permissions")
    public BaseResponse<PermissionRes> getPlusOperatorGoodsPermissions(@PathVariable(value = "companyNo") Integer companyNo) {
        return BaseResponseFactory.create(goodsPermissionService.getPlusOperatorGoodsPermissions(companyNo));
    }

}
