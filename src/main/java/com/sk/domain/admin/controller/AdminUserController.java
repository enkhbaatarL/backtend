package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.V1AdminController;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.UserReq;
import com.sk.domain.admin.dto.response.UserInfoRes;
import com.sk.domain.admin.dto.response.UserRes;
import com.sk.domain.admin.service.AdminUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "01. 사용자 API", description = "사용자 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "사용자 목록 원장테이블 조회 API", description = "사용자 목록을 원장 테이블에서 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping()
    public BaseResponse<PaginationRes<UserInfoRes>> getUserInfos(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyCode", required = true) String companyCode,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "emailAddress", required = false) String emailAddress
    ) {
        return BaseResponseFactory.create(adminUserService.getUserInfos(page, size, companyCode, userName, emailAddress));
    }

    @Operation(summary = "사용자 등록 API", description = "사용자 정보를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/")
    public BaseResponse<List<UserRes>> postUserInfo(
        @RequestBody @Valid List<UserReq.Insert> userReq
    ) {
        return BaseResponseFactory.create(adminUserService.postUserInfo(userReq));
    }
}