package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationExtensionRes;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyGoodsMapReq;
import com.sk.domain.admin.dto.request.company.CompanyProviderMapReq;
import com.sk.domain.admin.dto.request.company.CompanyReq;
import com.sk.domain.admin.dto.response.company.AuthGoodsMapCntRes;
import com.sk.domain.admin.dto.response.company.AuthGoodsMapRes;
import com.sk.domain.admin.dto.response.company.AuthGoodsMapSettingRes;
import com.sk.domain.admin.dto.response.company.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.company.CompanyGoodsMapDetailRes;
import com.sk.domain.admin.dto.response.company.CompanyGoodsMapRes;
import com.sk.domain.admin.dto.response.company.CompanyProviderMapDetailRes;
import com.sk.domain.admin.dto.response.company.CompanyProviderMapRes;
import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.CompanyTypeAndCompanyRes;
import com.sk.domain.admin.dto.response.company.GoodsWithDataCntRes;
import com.sk.domain.admin.dto.response.company.GroupCompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.admin.dto.response.company.ProviderWithGoodsCntRes;
import com.sk.domain.admin.service.AdminCompanyService;
import com.sk.domain.mi.dto.response.MiCompanyLogoRes;
import com.sk.domain.mi.service.MiCompanyLogoService;
import com.sk.domain.shared.enums.ActivationStatus;
import com.sk.domain.shared.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@AccessIpCheck
@Tag(name = "01. 고객사 API", description = "고객사 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/company-management")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final AdminCompanyService adminCompanyService;
    private final MiCompanyLogoService miCompanyLogoService;

    @ApiOrder(1)
    @Operation(summary = "그룹사 목록 조회 API (without pagination)", description = "그룹사 목록을 조회합니다. <br>'companyNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/group-companies")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<List<GroupCompanyRes>> getGroupCompanies() {
        return BaseResponseFactory.create(adminCompanyService.getGroupCompanies());
    }

    @ApiOrder(2)
    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
        @Parameter(name = "companyNm", description = "검색할 고객사 명 (like search)")
    })
    @Operation(summary = "관리사 목록 조회 API (with pagination)", description = "관리사 목록을 조회합니다. <br>페이지네이션 처리되어 'companyNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/owner-companies")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<CompanyRes>> getOwnerCompanies(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @RequestParam(value = "companyNm", required = false) String companyNm
    ) {
        return BaseResponseFactory.create(adminCompanyService.getOwnerCompanies(page, size, companyNm));
    }

    @ApiOrder(3)
    @Operation(summary = "관리사 등록 API", description = "관리사를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/owner-company")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyRes> postCompany(@RequestBody @Valid CompanyReq.OwnerInsert companyReq) {
        return BaseResponseFactory.create(adminCompanyService.postOwnerCompany(companyReq));
    }

    @ApiOrder(4)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "관리사 삭제 API", description = "관리사를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/owner-companies/{companyNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyRes> deleteOwnerCompany(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.deleteOwnerCompany(companyNo));
    }

    @ApiOrder(5)
    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
        @Parameter(name = "companyNm", description = "검색할 고객사 명 (like search)"),
        @Parameter(name = "activationSts", description = "검색할 활성화 옵션 (null: 전체, ACTIVE: 활성화, INACTIVE: 비활성화)")
    })
    @Operation(summary = "고객사 목록 조회 API (with pagination)", description = "고객사 목록을 조회합니다. <br>페이지네이션 처리되어 'companyNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<CompanyRes>> getCompanies(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @RequestParam(value = "companyNm", required = false) String companyNm,
        @RequestParam(value = "activationSts", required = false) ActivationStatus activationSts
    ) {
        return BaseResponseFactory.create(adminCompanyService.getCompanies(page, size, companyNm, activationSts));
    }

    @ApiOrder(6)
    @Operation(summary = "고객사 등록 API", description = "고객사를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/company")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyRes> postCompany(@RequestBody @Valid CompanyReq.Insert companyReq) {
        return BaseResponseFactory.create(adminCompanyService.postCompany(companyReq));
    }

    @ApiOrder(7)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사 상세 조회 API", description = "고객사를 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyRes> getCompanyDetail(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getCompanyDetail(companyNo));
    }

    @ApiOrder(8)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사 수정 API", description = "고객사를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/companies/{companyNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyRes> patchCompany(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @RequestBody @Valid CompanyReq.Update companyReq
    ) {
        return BaseResponseFactory.create(adminCompanyService.patchCompany(companyNo, companyReq));
    }

    @ApiOrder(9)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사 최신 수정 데이터 조회 API", description = "고객사 상세 화면에서 고객사 정보, 프로바이더 정보, 회원 정보를 포함하여 최신 수정 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/latest-updated")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<LatestUpdatedRes> getCompanyLatestUpdated(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getCompanyLatestUpdated(companyNo));
    }

    @ApiOrder(10)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 목록 조회 API (without pagination)", description = "고객사와 매핑된 프로바이더 목록을 조회합니다.<br>'providerNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<List<ProviderWithGoodsCntRes>> getProvidersMappedToCompany(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getProvidersMappedToCompany(companyNo));
    }

    @ApiOrder(11)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 등록 API", description = "고객사와 프로바이더를 매핑하여 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/companies/{companyNo}/provider")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyProviderMapRes> postProviderMappedToCompany(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @RequestBody @Valid CompanyProviderMapReq.Insert companyProviderMapReq
    ) {
        return BaseResponseFactory.create(adminCompanyService.postProviderMappedToCompany(companyNo, companyProviderMapReq));
    }

    @ApiOrder(12)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 상세 조회 API", description = "고객사와 매핑되어 있는 프로바이더를 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyProviderMapDetailRes> getProviderDetail(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(adminCompanyService.getProviderDetail(companyNo, providerNo));
    }

    @ApiOrder(13)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더 매핑 수정 API", description = "고객사와 프로바이더 매핑을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/companies/{companyNo}/providers/{providerNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyProviderMapRes> patchProviderMappedToCompany(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @RequestBody @Valid CompanyProviderMapReq.Update companyProviderMapReq
    ) {
        return BaseResponseFactory.create(adminCompanyService.patchProviderMappedToCompany(companyNo, providerNo, companyProviderMapReq));
    }

    @ApiOrder(14)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더 최신 수정 데이터 조회 API", description = "고객사-프로바이더 상세 화면에서 프로바이더 정보, 상품 정보를 포함하여 최신 수정 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/latest-updated")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<LatestUpdatedRes> getProviderLatestUpdated(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getProviderLatestUpdated(companyNo, providerNo));
    }

    @ApiOrder(15)
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
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<GoodsWithDataCntRes>> getGoodsMappedToCompany(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @RequestParam(value = "search", required = false) String search,
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getGoodsMappedToCompany(page, size, companyNo, providerNo, search));
    }

    @ApiOrder(16)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 매핑 등록 API", description = "고객사와 상품을 매핑하여 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/companies/{companyNo}/providers/{providerNo}/goods")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyGoodsMapRes> postGoodsMappedToCompany(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @RequestBody @Valid CompanyGoodsMapReq companyGoodsMapReq
    ) {
        return BaseResponseFactory.create(adminCompanyService.postGoodsMappedToCompany(companyNo, providerNo, companyGoodsMapReq));
    }

    @ApiOrder(17)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호"),
        @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 매핑 상세 조회 API", description = "고객사와 매핑되어 있는 상품을 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<CompanyGoodsMapDetailRes> getGoodsDetail(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getGoodsDetail(companyNo, providerNo, goodsNo));
    }

    @ApiOrder(18)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호"),
        @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 최신 수정 데이터 조회 API", description = "고객사-프로바이더-상품 상세 화면에서 고객사 정보, 프로바이더 정보, 회원 정보를 포함하여 최신 수정 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}/latest-updated")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<LatestUpdatedRes> getGoodsLatestUpdated(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getGoodsLatestUpdated(companyNo, providerNo, goodsNo));
    }

    @ApiOrder(19)
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
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationExtensionRes<AuthGoodsMapRes, AuthGoodsMapCntRes>> getAuthGoods(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo
    ) {
        return BaseResponseFactory.create(adminCompanyService.getAuthGoods(page, size, companyNo, providerNo, goodsNo));
    }

    @ApiOrder(20)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호"),
        @Parameter(name = "providerNo", description = "프로바이더 번호"),
        @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "고객사-프로바이더-상품 사용자 권한 설정 수정 API", description = "고객사와 매핑되어 있는 상품에 대한 사용자 권한 설정을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/companies/{companyNo}/providers/{providerNo}/goods/{goodsNo}/permissions")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<AuthGoodsMapSettingRes> patchAuthGoods(
        @PathVariable(value = "companyNo") @Min(value = 1) Integer companyNo,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo,
        @RequestBody @Valid AuthGoodsMapReq authGoodsMapReq
    ) {
        return BaseResponseFactory.create(adminCompanyService.patchGoodsAuth(companyNo, providerNo, goodsNo, authGoodsMapReq));
    }

    @ApiOrder(21)
    @Operation(summary = "고객사별 부서 목록 조회 API", description = "드랍다운 메뉴 필터에 쓰일 테이블 고객사, 부서 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/department/all")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<List<CompanyDepartmentRes>> getCompaniesAndDepartments() {
        return BaseResponseFactory.create(adminCompanyService.getCompaniesAndDepartments());
    }

    @ApiOrder(22)
    @Operation(summary = "고객사, 관리사 목록 일괄 조회 API", description = "드랍다운 메뉴 필터에 쓰일 테이블 고객사, 관리사 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/all")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<List<CompanyTypeAndCompanyRes>> getCompanyTypeAndCompanies() {
        return BaseResponseFactory.create(adminCompanyService.getCompanyTypeAndCompanies());
    }

    @ApiOrder(23)
    @Operation(summary = "등록된 모든 고객사 + 관리사 목록 조회 API(without pagination)", description = "드랍다운 메뉴 필터에 쓰일 고객사 + 관리사 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/list")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<List<CompanyRes>> getCompanyList() {
        return BaseResponseFactory.create(adminCompanyService.getCompanyList());
    }

    @ApiOrder(24)
    @Operation(
        summary = "고객사 로고 조회 API",
        description = """
            경로로 지정한 고객사의 로고 정보를 조회합니다. <br>
            플랫폼 관리자가 여러 고객사의 로고를 관리하는 화면에서 사용합니다. <br>

            <br><br>
            ### 자사 전용 API와의 차이
            회사 관리자가 자사 로고만 다루는 API는 `고객사 로고 API`(`/companies/me/logo`)에 별도로 있습니다.
            | | `/companies/me/logo` | `/companies/{companyNo}/logo` |
            |---|---|---|
            | 대상 회사 | 토큰 컨텍스트로 결정 | **경로 변수로 지정** |
            | 허용 역할 | `ADMIN`, `OPERATOR` | **`ADMIN` 전용** |
            | 용도 | 자사 로고 관리 | 플랫폼 관리자의 고객사별 로고 관리 |

            <br><br>
            ### 응답 필드
            - `customerCompanyNo`: 경로로 지정한 고객회사번호
            - `logoUrl`: 로고 이미지 URL. **전용 로고가 없으면 `null`** 이며, 이때 화면은 FE 기본 로고를 사용합니다.

            <br><br>
            ### logoUrl 형식
            FE는 형태를 구분할 필요 없이 받은 문자열을 그대로 `img` 태그의 `src`에 넣으면 됩니다. <br>

            <br><br>
            ### 오류
            존재하지 않거나 삭제된 고객회사번호를 지정하면 HTTP `400`(`RSC-001`)입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/companies/{companyNo}/logo")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN)
    })
    public BaseResponse<MiCompanyLogoRes> getCompanyLogo(
        @PathVariable @Parameter(description = "고객회사번호", example = "1") @Min(value = 1) Integer companyNo
    ) {
        return BaseResponseFactory.create(miCompanyLogoService.getLogo(companyNo));
    }

    @ApiOrder(25)
    @Operation(
        summary = "고객사 로고 등록/변경 API",
        description = """
            경로로 지정한 고객사의 로고를 등록하거나 교체합니다. <br>
            **등록과 교체를 구분하지 않습니다.** 기존 로고 유무와 무관하게, 호출하면 요청한 파일이 해당 고객사의 현재 로고가 됩니다. <br>

            <br><br>
            ### 요청 형식
            `multipart/form-data`이며 파일 파트 이름은 `file`입니다.
            ```
            PUT /api/v1/admin/company-management/companies/1/logo
            Content-Type: multipart/form-data

            file: (binary)
            ```

            <br><br>
            ### 파일 제약
            - **형식**: PNG, JPEG만 허용합니다. (SVG, WEBP, GIF, BMP 불가)
            - **크기**: 기본 3MB 이하입니다. (`mi.company-logo.max-file-size` 설정으로 환경별 조정 가능)
            - **파일명**: 확장자가 있어야 합니다.

            형식 판별은 **파일명 확장자나 `Content-Type` 헤더가 아니라 파일 내용으로** 합니다. <br>
            둘 다 클라이언트가 임의로 보낼 수 있으므로, 실제로 디코딩 가능한 이미지인지를 검사합니다. <br>
            따라서 확장자만 `.png`로 바꾼 비이미지 파일은 거부됩니다. <br>
            SVG를 막는 이유는 스크립트 실행이 가능한 XML이라, 서비스 도메인에서 직접 열릴 경우 동일 출처 스크립트 실행 경로가 되기 때문입니다.

            <br><br>
            ### 교체 시 동작
            새 파일을 먼저 업로드한 뒤, **교체 이전 시점에 활성 상태였던 로고 파일을 모두 삭제**합니다. <br>
            따라서 호출 이후 해당 고객사의 활성 로고는 항상 1건이며, 이전 로고 URL은 즉시 무효가 됩니다.

            <br><br>
            ### 응답 필드
            - `customerCompanyNo`: 로고가 등록된 고객회사번호
            - `logoUrl`: 새로 등록된 로고의 URL. 이 값으로 화면을 즉시 갱신할 수 있습니다.

            <br><br>
            ### 오류
            아래 오류는 모두 HTTP `400`이며 응답 본문의 `code`로 구분합니다.
            | code | 상황 |
            |---|---|
            | `RSC-001` | 존재하지 않거나 삭제된 고객회사번호 |
            | `FILE-001` | 파일이 없거나, PNG/JPEG가 아니거나, 파일명에 확장자가 없음 |
            | `FILE-003` | 허용 크기 초과 |

            권한이 없으면 HTTP `403`입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "등록/교체 성공")
    @PutMapping(value = "/companies/{companyNo}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN)
    })
    public BaseResponse<MiCompanyLogoRes> changeCompanyLogo(
        @PathVariable @Parameter(description = "고객회사번호", example = "1") @Min(value = 1) Integer companyNo,
        @Parameter(description = "로고 이미지 파일 (PNG 또는 JPEG, 기본 3MB 이하)") @RequestPart(value = "file") MultipartFile file
    ) {
        return BaseResponseFactory.create(miCompanyLogoService.changeLogo(companyNo, file));
    }

    @ApiOrder(26)
    @Operation(
        summary = "고객사 로고 해제 API",
        description = """
            경로로 지정한 고객사의 전용 로고를 해제하여 FE 기본 로고를 사용하도록 되돌립니다. <br>
            잘못 등록한 로고를 되돌리는 용도입니다. <br>

            <br><br>
            ### 동작
            해당 고객사의 활성 로고 파일을 모두 삭제 처리하고 오브젝트 스토리지에서도 제거합니다. <br>
            **멱등입니다.** 등록된 로고가 없는 상태에서 호출해도 오류 없이 성공합니다.

            <br><br>
            ### 응답 필드
            - `customerCompanyNo`: 대상 고객회사번호
            - `logoUrl`: 해제 후이므로 **항상 `null`** 입니다.

            <br><br>
            ### 오류
            존재하지 않거나 삭제된 고객회사번호를 지정하면 HTTP `400`(`RSC-001`)입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "해제 성공")
    @DeleteMapping("/companies/{companyNo}/logo")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN)
    })
    public BaseResponse<MiCompanyLogoRes> removeCompanyLogo(
        @PathVariable @Parameter(description = "고객회사번호", example = "1") @Min(value = 1) Integer companyNo
    ) {
        return BaseResponseFactory.create(miCompanyLogoService.removeLogo(companyNo));
    }

}
