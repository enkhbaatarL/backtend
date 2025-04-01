package com.sk.domain.admin.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequirePermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationExtensionRes;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyProviderMapReq;
import com.sk.domain.admin.dto.request.company.CompanyReq;
import com.sk.domain.admin.dto.response.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.company.*;
import com.sk.domain.admin.service.AdminCompanyService;
import com.sk.domain.shared.enums.ActivationStatus;
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

/**
 * todo...
 *  1. auto compelete ? fe 처리 가능? -> 처리가능
 *  2.
 */
@Tag(name = "01. 고객사 API", description = "고객사 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/company-management")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final AdminCompanyService adminCompanyService;

    @ApiOrder(1)
    @Operation(summary = "그룹사 목록 조회 API (without pagination)", description = "그룹사 목록을 조회합니다. <br>'companyNm' 필드를 기준으로 오름차순(ASC) 정렬되며, 우선순위는 숫자 > 알파벳 > 한국어 순입니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/group-companies")
    public BaseResponse<List<GroupCompanyRes>> getGroupCompanies() {
        return BaseResponseFactory.create(adminCompanyService.getGroupCompanies());
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
            @Parameter(name = "companyNm", description = "검색할 고객사 명 (like search)"),
            @Parameter(name = "activationSts", description = "검색할 활성화 옵션 (null: 전체, ACTIVE: 활성화, INACTIVE: 비활성화)")
    })
    @Operation(summary = "고객사 목록 조회 API (with pagination)", description = "고객사 목록을 조회합니다. <br>페이지네이션 처리되어 'companyNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies")
    public BaseResponse<PaginationRes<CompanyRes>> getCompanies(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyNm", required = false) String companyNm,
            @RequestParam(value = "activationSts", required = false) ActivationStatus activationSts) {
        return BaseResponseFactory.create(adminCompanyService.getCompanies(page, size, companyNm, activationSts));
    }

    @ApiOrder(3)
    @Operation(summary = "고객사 등록 API", description = "고객사를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/company")
    public BaseResponse<CompanyRes> postCompany(@RequestBody @Valid CompanyReq.Insert companyReq) {
        return BaseResponseFactory.create(adminCompanyService.postCompany(companyReq));
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사 상세 조회 API", description = "고객사를 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}")
    public BaseResponse<CompanyRes> getCompanyDetail(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo) {
        return BaseResponseFactory.create(adminCompanyService.getCompanyDetail(companyNo));
    }

    @ApiOrder(5)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사 수정 API", description = "고객사를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/companies/{companyNo}")
    public BaseResponse<CompanyRes> patchCompany(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @RequestBody @Valid CompanyReq.Update companyReq) {
        return BaseResponseFactory.create(adminCompanyService.patchCompany(companyNo, companyReq));
    }

    @ApiOrder(6)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사 최신 수정 데이터 조회 API", description = "고객사 상세 화면에서 고객사 정보, 프로바이더 정보, 회원 정보를 포함하여 최신 수정 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/latest-updated")
    public BaseResponse<LatestUpdatedRes> getCompanyLatestUpdated(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo) {
        return BaseResponseFactory.create(adminCompanyService.getCompanyLatestUpdated(companyNo));
    }

    @ApiOrder(7)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 목록 조회 API (without pagination)", description = "고객사와 매핑된 프로바이더 목록을 조회합니다.<br>'providerNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers")
    public BaseResponse<List<ProviderWithGoodsCntRes>> getProvidersMappedToCompany(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo) {
        return BaseResponseFactory.create(adminCompanyService.getProvidersMappedToCompany(companyNo));
    }

    @ApiOrder(8)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 등록 API", description = "고객사와 프로바이더를 매핑하여 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/companies/{companyNo}/provider")
    public BaseResponse<CompanyProviderMapRes> postProviderMappedToCompany(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @RequestBody @Valid CompanyProviderMapReq.Insert companyProviderMapReq) {
        return BaseResponseFactory.create(adminCompanyService.postProviderMappedToCompany(companyNo, companyProviderMapReq));
    }

    @ApiOrder(9)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 상세 조회 API", description = "고객사와 매핑되어 있는 프로바이더를 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}")
    public BaseResponse<CompanyProviderMapDetailRes> getProviderDetail(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(adminCompanyService.getProviderDetail(companyNo, providerNo));
    }

    @ApiOrder(10)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 수정 API", description = "고객사와 프로바이더 매핑을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/companies/{companyNo}/providers/{providerNo}")
    public BaseResponse<CompanyProviderMapRes> patchProviderMappedToCompany(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @RequestBody @Valid CompanyProviderMapReq.Update companyProviderMapReq) {
        return BaseResponseFactory.create(adminCompanyService.patchProviderMappedToCompany(companyNo, providerNo, companyProviderMapReq));
    }

    @ApiOrder(11)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더 최신 수정 데이터 조회 API", description = "고객사-프로바이더 상세 화면에서 프로바이더 정보, 상품 정보를 포함하여 최신 수정 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/latest-updated")
    public BaseResponse<LatestUpdatedRes> getProviderLatestUpdated(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(adminCompanyService.getProviderLatestUpdated(companyNo, providerNo));
    }

    @ApiOrder(12)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
            @Parameter(name = "search", description = "검색어 (상품명 또는 상품 설명)"),
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호"),
    })
    @Operation(summary = "고객사-프로바이더-상품 매핑 목록 조회 API (with pagination)", description = "고객사와 매핑되어있는 프로바이더의 상품 내역 중에 고객사가 등록한 상품 목록을 조회합니다.<br>'goodsNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/goods")
    public BaseResponse<PaginationRes<GoodsWithDataCntRes>> getGoodsMappedToCompany(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "search", required = false) String search,
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(adminCompanyService.getGoodsMappedToCompany(page, size, companyNo, providerNo, search));
    }

    @ApiOrder(13)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 매핑 등록 API", description = "고객사와 상품을 매핑하여 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/companies/{companyNo}/providers/{providerNo}/goods")
    public BaseResponse<CompanyGoodsMapRes> postGoodsMappedToCompany(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @RequestBody @Valid CompanyGoodsMapReq companyGoodsMapReq) {
        return BaseResponseFactory.create(adminCompanyService.postGoodsMappedToCompany(companyNo, providerNo, companyGoodsMapReq));
    }

    @ApiOrder(14)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 매핑 상세 조회 API", description = "고객사와 매핑되어 있는 상품을 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}")
    public BaseResponse<CompanyGoodsMapDetailRes> getGoodsDetail(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(adminCompanyService.getGoodsDetail(companyNo, providerNo, goodsNo));
    }

    @ApiOrder(15)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 최신 수정 데이터 조회 API", description = "고객사-프로바이더-상품 상세 화면에서 고객사 정보, 프로바이더 정보, 회원 정보를 포함하여 최신 수정 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}/latest-updated")
    public BaseResponse<LatestUpdatedRes> getGoodsLatestUpdated(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(adminCompanyService.getGoodsLatestUpdated(companyNo, providerNo, goodsNo));
    }

    @ApiOrder(16)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 사용자 권한 설정 조회 API (with pagination)", description = "고객사와 매핑되어 있는 상품에 대한 사용자 권한 설정을 조회합니다.<br>'userNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}/permissions")
    public BaseResponse<PaginationExtensionRes<AuthGoodsMapRes, AuthGoodsMapCntRes>> getAuthGoods(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(adminCompanyService.getAuthGoods(page, size, companyNo, providerNo, goodsNo));
    }

    @ApiOrder(17)
    @Parameters({
            @Parameter(name = "companyNo", description = "고객회사 번호"),
            @Parameter(name = "providerNo", description = "프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 사용자 권한 설정 수정 API", description = "고객사와 매핑되어 있는 상품에 대한 사용자 권한 설정을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}/permissions")
    public BaseResponse<AuthGoodsMapSettingRes> patchAuthGoods(
            @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo,
            @RequestBody @Valid AuthGoodsMapReq authGoodsMapReq) {
        return BaseResponseFactory.create(adminCompanyService.patchGoodsAuth(companyNo, providerNo, goodsNo, authGoodsMapReq));
    }

    @ApiOrder(18)
    @Operation(summary = "고객사, 부서 목록 조회 API", description = "드랍다운 메뉴 필터에 쓰일 테이블 고객사, 부서 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/company/department/all")
    public BaseResponse<List<CompanyDepartmentRes>> getCompaniesAndDepartments() {
        return BaseResponseFactory.create(adminCompanyService.getCompaniesAndDepartments());
    }

}
