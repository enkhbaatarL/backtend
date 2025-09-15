package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryDataRes;
import com.sk.domain.mi.service.MiDashboardHistoryService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "01-2. AI Forecast (구 Dashboard) History API", description = "대시보드 히스토리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardHistoryDedicatedController {

    private final MiDashboardHistoryService dashboardHistoryService;

    @ApiOrder(11)
    @Parameters({
        @Parameter(name = "settingDetailNo", description = "설정 상세 번호"),
        @Parameter(name = "fulfillDate", description = "예측수행일자")
    })
    @Operation(summary = "대시보드 히스토리 상세 데이터 조회 API", description = "대시보드 히스토리 상세 데이터를 조회합니다. (예측가 + 실제가 or 선물가 조합으로 데이터가 생성됩니다.)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/histories/setting-details/{settingDetailNo}")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<HistoryDataRes>> getHistoryData(
        @PathVariable(value = "settingDetailNo") Integer settingDetailNo,
        @RequestParam(value = "fulfillDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate fulfillDate
    ) {
        return BaseResponseFactory.create(dashboardHistoryService.getHistoryData(settingDetailNo, fulfillDate));
    }

    @ApiOrder(12)
    @Parameters({
        @Parameter(name = "settingDetailNo", description = "설정 상세 번호"),
        @Parameter(name = "fulfillDate", description = "예측수행일자")
    })
    @Operation(summary = "대시보드 히스토리 상세 데이터 다운로드 API", description = "대시보드 히스토리 상세 데이터를 다운로드 합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/histories/setting-details/{settingDetailNo}/download")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public ResponseEntity<InputStreamResource> downloadHistoryData(
        @PathVariable(value = "settingDetailNo") Integer settingDetailNo,
        @RequestParam(value = "fulfillDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate fulfillDate
    ) {
        HttpHeaders headers = new HttpHeaders();
        String title = "dashboard_history_%s_%s.xlsx".formatted(fulfillDate.toString(), DateUtil.currentYmdHms());
        headers.setContentDisposition(ContentDisposition.attachment().filename(title).build());

        ByteArrayInputStream in = dashboardHistoryService.downloadHistoryData(settingDetailNo, fulfillDate);

        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(in));
    }

}
