package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.marketData.MiMarketDataRes;
import com.sk.domain.mi.service.MiMarketDataService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "03. 시장 데이터 API", description = "시장 데이터 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/market-data")
@RequiredArgsConstructor
public class MiMarketDataController {

    private final MiMarketDataService miMarketDataService;

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "고객회사 번호")
    })
    @Operation(summary = "마켓 데이터 조회 API", description = "마켓 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "마켓 데이터 조회 성공")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY, companyParam = "customerCompanyNo")
    @GetMapping("/company/{customerCompanyNo}/market-datas")
    public BaseResponse<List<MiMarketDataRes>> getMarketDatas(@PathVariable("customerCompanyNo") Integer customerCompanyNo,
            @UserResolve User user) {
        return BaseResponseFactory.create(miMarketDataService.getMarketDatas(customerCompanyNo, user));
    }
}
