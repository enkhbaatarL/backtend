package com.sk.domain.admin.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.CompanyInfoReq;
import com.sk.domain.admin.dto.response.CompanyInfoRes;
import com.sk.domain.admin.service.CompanyInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "02. 고객사 API", description = "고객사 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/company-info")
@RequiredArgsConstructor
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;

    @Operation(summary = "고객사 목록 조회 API", description = "고객사 목록을 조회합니다. with pagination")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies")
    public BaseResponse<PaginationRes<CompanyInfoRes>> getCompaniesInfo(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size
    ) {
        return BaseResponseFactory.create(companyInfoService.getCompaniesInfo(page, size));
    }

    @Operation(summary = "고객사 등록 API", description = "고객사를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/company")
    public BaseResponse<CompanyInfoRes> postCompany(@RequestBody @Valid CompanyInfoReq companyInfoReq) {
        return BaseResponseFactory.create(companyInfoService.postCompany(companyInfoReq));
    }

}
