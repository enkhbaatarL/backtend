package com.sk.domain.shared.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.service.SchedulerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * 스케쥴러용 API
 */
@RestController
@RequestMapping("/api/v1/api/scheduler")
@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
@RequiredArgsConstructor
public class SchedulerController {
    private final SchedulerService schedulerService;

    @ApiOrder(1)
    @Operation(summary = "GDI 탈퇴 유저 soft delete 처리 API", description = "GDI 탈퇴 유저 soft delete 처리 API 입니다.")
    @DeleteMapping("/group-users")
    public BaseResponse<Boolean> processGdiUserSoftDelete() {
        return BaseResponseFactory.create(schedulerService.processGdiUserSoftDelete());
    }
    
    @ApiOrder(2)
    @Operation(summary = "알림 발송 API", description = "배치에 쓰일 API로 임계치 확인 후 해당 유저에게 알림(이메일) 발송 API 입니다.")
    @PostMapping("/notification")
    public BaseResponse<Boolean> createNotification() {
        return BaseResponseFactory.create(schedulerService.createNotification());
    }
}
