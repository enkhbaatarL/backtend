package com.sk.domain.mi.controller;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.UserResolve;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.notification.NotificationSettingReq;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingGoodsDataSourceRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingRes;
import com.sk.domain.mi.service.MiNotificationService;
import com.sk.domain.shared.entity.master.tmd.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "06. 알림 관리 API", description = "알림 관리 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/notification-management")
@RequiredArgsConstructor
@Slf4j
public class MiNotificationController {

    private final MiNotificationService miNotificationService;

    @Parameters({
            @Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호", required = true),
    })
    @ApiOrder(1)
    @Operation(summary = "알림 설정", description = "알림을 설정합니다.")
    @ApiResponse(responseCode = "200", description = "알림 설정 성공")
    @PostMapping("/goodsdata/{goodsDataSourceNo}/notification-setting")
    public BaseResponse<MiNotificationSettingRes> createNotificationSetting(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
            @RequestBody @Valid NotificationSettingReq.Insert notifications,
            @UserResolve User user,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            log.error("알림 설정 오류: {}", bindingResult.getFieldErrors());
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, bindingResult.getFieldErrors().get(0).getDefaultMessage());
        }

        return BaseResponseFactory
                .create(miNotificationService.createNotificationSetting(goodsDataSourceNo, notifications, user));
    }

    @Parameters({
            @Parameter(name = "page", description = "페이지 번호", required = true),
            @Parameter(name = "size", description = "페이지 크기", required = true)
    })
    @ApiOrder(2)
    @Operation(summary = "알림 설정한 정형 데이터 목록 조회", description = "알림 설정한 정형 데이터 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "알림 조회 성공")
    @GetMapping("/notification-settings")
    public BaseResponse<PaginationRes<MiNotificationSettingGoodsDataSourceRes>> getNotificationSettings(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "search", required = false) String search,
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.getNotificationSettingsGoodsDataSource(page, size, search, user));
    }

    //TODO: 커서기반 페이지네이션 필요
    @Parameters({
            @Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호(PK)", required = true),
            @Parameter(name = "page", description = "페이지 번호", required = true),
            @Parameter(name = "size", description = "페이지 크기", required = true)
    })
    @ApiOrder(3)
    @Operation(summary = "정형 데이터 알림 설정 리스트 조회", description = "정형 데이터 알림 설정 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "알림 조회 성공")
    @GetMapping("/goodsdata/{goodsDataSourceNo}/notification-settings")
    public BaseResponse<PaginationRes<MiNotificationSettingRes>> getNotificationSettings(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
            @UserResolve User user,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size) {
        return BaseResponseFactory
                .create(miNotificationService.getNotificationSettings(page, size, goodsDataSourceNo, user));
    }

    @Parameters({
            @Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호(PK)", required = true),
            @Parameter(name = "notificationSettingNo", description = "알림 설정 번호(PK)", required = true)
    })
    @ApiOrder(4)
    @Operation(summary = "알림 설정 상세 조회", description = "상품 데이터의 알림 설정을 상세조회합니다.")
    @ApiResponse(responseCode = "200", description = "알림 조회 성공")
    @GetMapping("/goodsdata/{goodsDataSourceNo}/notification-settings/{notificationSettingNo}")
    public BaseResponse<MiNotificationSettingRes> getNotificationSetting(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
            @PathVariable(value = "notificationSettingNo") @Min(value = 1) Integer notificationSettingNo,
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.getNotificationSetting(goodsDataSourceNo, notificationSettingNo, user));
    }

    @Parameters({
            @Parameter(name = "goodsDataSourceNo", description = "상품 데이터 소스 번호(PK)", required = true),
            @Parameter(name = "notificationSettingNo", description = "알림 설정 번호(PK)", required = true)
    })
    @ApiOrder(5)
    @Operation(summary = "알림 설정 삭제", description = "알림 설정을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "알림 삭제 성공")
    @DeleteMapping("/goodsdata/{goodsDataSourceNo}/notification-settings/{notificationSettingNo}")
    public BaseResponse<MiNotificationSettingRes> deleteNotificationSetting(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
            @PathVariable(value = "notificationSettingNo") @Min(value = 1) Integer notificationSettingNo,
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.deleteNotificationSetting(goodsDataSourceNo, notificationSettingNo, user));
    }

    //TODO: 커서기반 페이지네이션 필요
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호", required = true),
            @Parameter(name = "size", description = "페이지 크기", required = true)
    })
    @ApiOrder(6)
    @Operation(summary = "알림 발송 내역 리스트 조회", description = "알림 발송 내역 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "알림 발송 내역 조회 성공")
    @GetMapping("/notifications")
    public BaseResponse<PaginationRes<MiNotificationRes>> getNotifications(
            @UserResolve User user,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size) {
        return BaseResponseFactory
                .create(miNotificationService.getNotifications(page, size, user));
    }

    @Parameters({
            @Parameter(name = "emailSendSn", description = "알림 발송 내역 번호(PK)", required = true)
    })
    @ApiOrder(7)
    @Operation(summary = "알림 발송 내역 상세보기", description = "알림 발송 내역 상세보기를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "알림 발송 내역 상세보기 조회 성공")
    @GetMapping("/notifications/{emailSendSn}")
    public BaseResponse<MiNotificationRes> getNotification(
            @PathVariable(value = "emailSendSn") @Min(value = 1) Integer emailSendSn,
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.getNotification(emailSendSn, user));
    }
}
