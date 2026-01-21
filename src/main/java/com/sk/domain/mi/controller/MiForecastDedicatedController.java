package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastSummaryRes;
import com.sk.domain.mi.service.MiForecastService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "18. AI Forecast API", description = "AI Forecast API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/forecast")
@RequiredArgsConstructor
public class MiForecastDedicatedController {
	private final MiForecastService miForecastService;

	@ApiOrder(2)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Operation(summary = "Forecast 기본 정보 조회", description = "Forecast 기본 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/info")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<ForecastInfoRes> getForecastInfo(
        @PathVariable Integer companyNo) {
		return BaseResponseFactory.create(miForecastService.getForecastInfo(companyNo));
	}

	@ApiOrder(5)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Operation(summary = "Forecast summary 목록 조회", description = "Forecast summary 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/summaries")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<List<ForecastSummaryRes>> getForecastSummaries(
			@PathVariable Integer companyNo) {
		return BaseResponseFactory.create(miForecastService.getForecastSummaries(companyNo));
	}
}
