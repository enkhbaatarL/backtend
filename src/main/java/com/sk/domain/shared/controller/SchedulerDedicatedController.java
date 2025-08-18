package com.sk.domain.shared.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.request.file.PdgImageReq;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 스케쥴러용 API
 */
@Tag(name = "06. Scheduler API", description = "스케쥴러 API")
@RestController
@RequestMapping("/api/v1/scheduler-management")
@RequiredArgsConstructor
public class SchedulerDedicatedController {
    private final SchedulerService schedulerService;

    @ApiOrder(2)
    @Operation(summary = "알림 발송 API", description = "배치에 쓰일 API로 임계치 확인 후 해당 유저에게 알림(이메일) 발송 API 입니다.")
    @PostMapping("/notification")
    @RequiredPermission(roles = {Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR, Role.ADMIN, Role.SYSTEM})
    @ApiResponse(responseCode = "200", description = "알림 발송 성공")
    public BaseResponse<Boolean> createNotification() {
        return BaseResponseFactory.create(schedulerService.createNotification());
    }

    @ApiOrder(3)
    @Operation(
        summary = "PDF파일 이미지 변환 API",
        description = """
            배치에 쓰일 API로 PDF파일을 이미지로 나누어 S3 업로드 합니다. <br>
            """
    )
    @PostMapping("/pdf-to-images")
    @RequiredPermission(roles = {Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR, Role.ADMIN, Role.SYSTEM})
    @ApiResponse(responseCode = "200", description = "PDF파일 이미지 변환 성공")
    public BaseResponse<Boolean> processPdfToImages(
        @RequestBody(required = false) PdgImageReq pdgImageReq
    ) {
        return BaseResponseFactory.create(schedulerService.processPdfToImages(pdgImageReq));
    }

    @ApiOrder(4)
    @Operation(summary = "고객사에서 요청한 Market Data 계산 후 Spread 테이블에 업로드", description = "고객사에서 요청한 Market Data 계산 후 Spread 테이블에 업로드 API 입니다.")
    @PostMapping("/market-data/calculate-spread")
    @RequiredPermission(roles = {Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR, Role.ADMIN, Role.SYSTEM})
    @ApiResponse(responseCode = "200", description = "고객사에서 요청한 Market Data 계산 후 Spread 테이블에 업로드 성공")
    public BaseResponse<Boolean> processTifFuturesSpread() {
        return BaseResponseFactory.create(schedulerService.processTifFuturesSpread());
    }

    @ApiOrder(5)
    @Operation(summary = "GIE 데이터의 과거 정보 계산", description = "GIE 데이터의 과거 정보 계산")
    @PostMapping("/market-data/calculate-gie-eu")
    @RequiredPermission(roles = {Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR, Role.ADMIN, Role.SYSTEM})
    @ApiResponse(responseCode = "200", description = "GIE 데이터의 과거 정보 계산후 업로드 성공")
    public BaseResponse<Boolean> processTifGiePast() {
        return BaseResponseFactory.create(schedulerService.processTifGiePast());
    }

}
