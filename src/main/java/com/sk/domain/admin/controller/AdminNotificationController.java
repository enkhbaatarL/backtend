package com.sk.domain.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.NotificationRes;
import com.sk.domain.admin.service.AdminNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
            @Parameter(name = "search", description = "검색어")
    })
    @Operation(summary = "알림 조회 API", description = "알림 조회 API 입니다.")
    @GetMapping("/notifications")
    public BaseResponse<PaginationRes<NotificationRes>> getNotifications(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "search", required = false) String search) {
        return BaseResponseFactory.create(adminNotificationService.getNotifications(page, size, search));
    }

}