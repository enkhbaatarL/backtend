package com.sk.domain.shared.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.request.rpa.ComponentInfoReq;
import com.sk.domain.shared.dto.request.rpa.ProviderDataColumnReq;
import com.sk.domain.shared.dto.response.rpa.ComponentInfoRes;
import com.sk.domain.shared.dto.response.rpa.ProviderDataColumnRes;
import com.sk.domain.shared.service.RpaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "05. RPA API", description = "RPA API")
@Slf4j
@RestController
@RequestMapping("/api/v1/rpa-management")
@RequiredArgsConstructor
public class RpaController {
    private final RpaService rpaService;

    @ApiOrder(1)
    @Operation(summary = "컴포넌트 생성", description = "컴포넌트를 생성합니다.")
    @ApiResponse(responseCode = "200", description = "컴포넌트 생성 성공")
    @PostMapping("/component")
    public BaseResponse<ComponentInfoRes> createComponentInfo(
            @RequestBody @Valid ComponentInfoReq.Insert componentInfoReq) {
        return BaseResponseFactory.create(rpaService.createComponentInfo(componentInfoReq));
    }

    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
    })
    @ApiOrder(2)
    @Operation(summary = "프로바이더 컴포넌트 설명 데이터 생성", description = "프로바이더 컴포넌트 설명 데이터를 생성합니다.")
    @ApiResponse(responseCode = "200", description = "프로바이더 컴포넌트 설명 데이터 생성 성공")
    @PostMapping("/providers/{providerNo}/data-columns")
    public BaseResponse<List<ProviderDataColumnRes>> createProviderDataColumn(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @RequestBody @Valid List<ProviderDataColumnReq> providerDataColumnsReq) {
        return BaseResponseFactory.create(rpaService.createProviderDataColumn(providerNo, providerDataColumnsReq));
    }

}
