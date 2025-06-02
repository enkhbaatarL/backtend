package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryDataRes;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryMetadataRes;
import com.sk.domain.mi.service.MiDashboardHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "11. 대시보드 히스토리 API", description = "대시보드 히스토리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardHistoryController {

    private final MiDashboardHistoryService dashboardHistoryService;

    /**
     * 대시보드 History Section
     * 1. 드랍다운 리스트 조회 API + meta?
     * 2. 데이터 리스트 조회 API (예측변화율, 실가변화율, 가장 높은 예측값, 가장 낮은 예측값, 가장 높은 실제값, 가장 낮은 실제값)
     * 3. 월별 데이터인 경우 리포트 다운로드 API
     */
    @ApiOrder(10)
    @Operation(summary = "대시보드 히스토리 메타데이터 API", description = "대시보드 히스토리 메타데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/history/metadata")
    public BaseResponse<HistoryMetadataRes> getForecastHistoryMetadata() {
        return BaseResponseFactory.create(dashboardHistoryService.getForecastHistoryMetadata());
    }

    @ApiOrder(11)
    @Operation(summary = "대시보드 히스토리 상세 데이터 조회 API", description = "대시보드 히스토리 상세 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/histories")
    public BaseResponse<List<HistoryDataRes>> getHistoryData(
        @RequestParam(value = "settingDetailNo") Integer settingDetailNo,
        @RequestParam(value = "fulfillDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate fulfillDate
    ) {
        return BaseResponseFactory.create(dashboardHistoryService.getHistoryData(settingDetailNo, fulfillDate));
    }

    @ApiOrder(12)
    @Operation(summary = "대시보드 히스토리 상세 데이터 다운로드 API", description = "대시보드 히스토리 상세 데이터를 다운로드 합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/histories/download")
    public Object downloadHistoryData(
        @RequestParam(value = "settingDetailNo") Integer settingDetailNo,
        @RequestParam(value = "fulfillDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate fulfillDate
    ) {
        return BaseResponseFactory.create(dashboardHistoryService.downloadHistoryData(settingDetailNo, fulfillDate));
    }

}
