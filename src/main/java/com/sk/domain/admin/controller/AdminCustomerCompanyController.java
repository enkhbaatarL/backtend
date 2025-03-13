package com.sk.domain.admin.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.CustomerCompanyReq;
import com.sk.domain.admin.dto.request.ProviderMappingReq;
import com.sk.domain.admin.dto.response.CompanyOverviewRes;
import com.sk.domain.admin.dto.response.CompanyRes;
import com.sk.domain.admin.dto.response.GroupCompanyRes;
import com.sk.domain.admin.dto.response.ProviderMappingRes;
import com.sk.domain.admin.service.AdminCustomerCompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// todo 체크
// 1. 고객사 목록 조회 API : 상태 파라미터
// 2. 고객사 상세 구성 조회 API : 수정일 업데이트 삭제 고려
// 3. 고객사 권한 조회 API : 고객사가 사용중인 권한? 프로세스 파악 불분명 / 상품매핑내역 체크? 프로바이더매핑내역 체크?
// 4. 회원삭제 버튼?
// 5. 프로바이더 목록 조회 제공 여부

@Tag(name = "01. 고객사 API", description = "고객사 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/company-info")
@RequiredArgsConstructor
public class AdminCustomerCompanyController {

    private final AdminCustomerCompanyService adminCustomerCompanyService;

    @Operation(summary = "그룹사 목록 조회 API", description = "그룹사 목록을 조회합니다. (sort : number -> alphabet -> korean ASC)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/group-companies")
    public BaseResponse<List<GroupCompanyRes>> getGroupCompanies() {
        return BaseResponseFactory.create(adminCustomerCompanyService.getGroupCompanies());
    }

    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
        @Parameter(name = "companyNm", description = "검색할 고객사 이름 (like 검색)")
    })
    @Operation(summary = "고객사 목록 조회 API", description = "고객사 목록을 조회합니다. with pagination (sort : 생성일 DESC)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies")
    public BaseResponse<PaginationRes<CompanyRes>> getCompanies(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @RequestParam(value = "companyNm", required = false) String companyNm
    ) {
        return BaseResponseFactory.create(adminCustomerCompanyService.getCompanies(page, size, companyNm));
    }

    @Operation(summary = "고객사 등록 API", description = "고객사를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/company")
    public BaseResponse<CompanyRes> postCompany(@RequestBody @Valid CustomerCompanyReq customerCompanyReq) {
        return BaseResponseFactory.create(adminCustomerCompanyService.postCompany(customerCompanyReq));
    }

    // todo 고객사 권한 조회 API

    @Operation(summary = "고객사 상세 구성 조회 API", description = "고객사의 상세 구성을 조회합니다. (고객사, 프로바이더 목록, 회원 목록)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/company/{companyNo}/overview")
    public BaseResponse<CompanyOverviewRes> getCompanyOverview(
        @PathVariable(value = "companyNo") @Min(value = 1)
        @Parameter(name = "companyNo", description = "고객회사번호") Integer companyNo
    ) {
        return BaseResponseFactory.create(adminCustomerCompanyService.getCompanyOverview(companyNo));
    }

    @Operation(summary = "고객사-프로바이더 매핑 등록 API", description = "특정 고객사가 사용할 데이터 프로바이더 매핑 등록한다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/company/{companyNo}/provider")
    public BaseResponse<ProviderMappingRes> postCompanyProvider(
        @PathVariable(value = "companyNo") @Min(value = 1)
        @Parameter(name = "companyNo", description = "고객회사번호") Integer companyNo,
        @RequestBody @Valid ProviderMappingReq providerMappingReq
    ) {
        return BaseResponseFactory.create(adminCustomerCompanyService.postCompanyProvider(companyNo, providerMappingReq));
    }

}
