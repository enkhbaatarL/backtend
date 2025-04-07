package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.notification.NotificationReq;
import com.sk.domain.mi.dto.response.notification.NotificationRes;
import com.sk.domain.mi.service.MiNotificationService;
import com.sk.domain.shared.entity.master.tmd.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "01. 사용자 API", description = "사용자 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/notification-management")
@RequiredArgsConstructor
@Slf4j
@ApiResponse(responseCode = "200", description = "알림 생성 성공")
public class MiNotificationController {

    private final MiNotificationService miNotificationService;

    @ApiOrder(1)
    @Operation(summary = "알림 생성", description = "알림을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "알림 생성 성공")
    @PostMapping("/notification/goodsdata/{goodsDataSourceNo}")
    public BaseResponse<List<NotificationRes>> createNotification(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
            @RequestBody @Valid NotificationReq.Insert notifications,
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.createNotification(notifications, goodsDataSourceNo, user));
    }

    @ApiOrder(2)
    @Operation(summary = "알림 조회", description = "알림을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "알림 조회 성공")
    @GetMapping("/notification/goodsdata/{goodsDataSourceNo}")
    public BaseResponse<List<NotificationRes>> getNotification(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,   
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.getNotification(goodsDataSourceNo, user));
    }

    @ApiOrder(3)
    @Operation(summary = "알림 삭제", description = "알림을 삭제합니다. (미사용)")
    @ApiResponse(responseCode = "200", description = "알림 삭제 성공")
    @DeleteMapping("/notification/goodsdata/{goodsDataSourceNo}")
    public BaseResponse<List<NotificationRes>> deleteNotification(
            @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
            @RequestBody @Valid NotificationReq.Delete notifications,
            @UserResolve User user) {
        return BaseResponseFactory
                .create(miNotificationService.deleteNotification(notifications, goodsDataSourceNo, user));
    }


    /**
     * 수정 기획 삭제
     */
    // @ApiOrder(4)
    // @Operation(summary = "알림 등록/수정/삭제", description = "알림을 등록/수정/삭제합니다. (Insert/Update/Delete 일괄 처리)")
    // @ApiResponse(responseCode = "200", description = "알림 수정 성공")
    // @PutMapping("/notification/goodsdata/{goodsDataSourceNo}")
    // public BaseResponse<List<NotificationRes>> updateNotification(
    //         @PathVariable(value = "goodsDataSourceNo") @Min(value = 1) Integer goodsDataSourceNo,
    //         @RequestBody @Valid NotificationReq.Update notifications,
    //         @UserResolve User user) {
    //     return BaseResponseFactory
    //             .create(miNotificationService.updateNotification(notifications, goodsDataSourceNo, user));
    // }
}
