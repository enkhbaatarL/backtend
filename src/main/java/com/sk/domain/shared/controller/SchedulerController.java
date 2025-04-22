package com.sk.domain.shared.controller;

import java.util.List;
import java.util.Map;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 스케쥴러용 API
 */
@Tag(name = "00. Scheduler API", description = "스케쥴러 API")
@RestController
@RequestMapping("/api/v1/api/scheduler-management")
@RequiredArgsConstructor
public class SchedulerController {
    private final SchedulerService schedulerService;

    @ApiOrder(1)
    @Operation(summary = "GDI 탈퇴 유저 soft delete 처리 API", description = "GDI 탈퇴 유저 soft delete 처리 API 입니다.")
    @DeleteMapping("/group-users")
    @ApiResponse(responseCode = "200", description = "GDI 탈퇴 유저 soft delete 처리 성공")
    public BaseResponse<Boolean> processGdiUserSoftDelete() {
        return BaseResponseFactory.create(schedulerService.processGdiUserSoftDelete());
    }

    @ApiOrder(2)
    @Operation(summary = "알림 발송 API", description = "배치에 쓰일 API로 임계치 확인 후 해당 유저에게 알림(이메일) 발송 API 입니다.")
    @PostMapping("/notification")
    @ApiResponse(responseCode = "200", description = "알림 발송 성공")
    public BaseResponse<Boolean> createNotification() {
        return BaseResponseFactory.create(schedulerService.createNotification());
    }

    @ApiOrder(3)
    @Operation(summary = "PDF파일 이미지 변환 API", description = "배치에 쓰일 API로 PDF파일을 이미지로 나누어 S3 업로드 합니다.")
    @PostMapping("/pdf-to-images")
    @ApiResponse(responseCode = "200", description = "알림 발송 성공")
    public BaseResponse<Boolean> processPdfToImages() {
        return BaseResponseFactory.create(schedulerService.processPdfToImages());
    }

}
