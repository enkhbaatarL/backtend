package com.sk.domain.admin.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationExtensionRes;
import com.sk.domain.admin.dto.response.notification.AdminNotificationCountRes;
import com.sk.domain.admin.dto.response.notification.AdminNotificationRes;
import com.sk.domain.admin.service.AdminNotificationService;
import com.sk.domain.shared.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@AccessIpCheck
@Tag(name = "05. 알림 API", description = "알림 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/notification-management")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "isSendSuccess", description = "발송 성공 여부"),
            @Parameter(name = "startDate", description = "조회 시작일 (YYYYMMDD), Default 어제", required = true),
            @Parameter(name = "endDate", description = "조회 종료일 (YYYYMMDD), Default 오늘", required = true)
    })
    @Operation(summary = "알림 조회 API", description = "알림 조회 API 입니다.")
    @GetMapping("/notifications")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationExtensionRes<AdminNotificationRes, AdminNotificationCountRes>> getNotifications(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "isSendSuccess", required = false) Boolean isSendSuccess,
            @RequestParam(value = "startDate", required = true) String startDate,
            @RequestParam(value = "endDate", required = true) String endDate) {
        return BaseResponseFactory.create(adminNotificationService.getNotifications(page, size, isSendSuccess, startDate, endDate));
    }

}
