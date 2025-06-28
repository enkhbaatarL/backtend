package com.sk.domain.admin.controller;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataFileReq;
import com.sk.domain.admin.dto.response.file.FileExistRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.dto.response.file.GoodsDataFileUploadRes;
import com.sk.domain.admin.dto.response.provider.ComponentInfoRes;
import com.sk.domain.admin.dto.response.provider.GoodsCompanyPermissionRes;
import com.sk.domain.admin.dto.response.provider.GoodsCreateRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsLiteRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.service.AdminGoodsDataService;
import com.sk.domain.admin.service.AdminGoodsService;
import com.sk.domain.admin.service.AdminProviderService;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@AccessIpCheck
@Tag(name = "03. 데이타 프로바이더 관리 API", description = "데이타 프로바이더 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/provider-management")
@RequiredArgsConstructor
public class AdminProviderController {
    private final AuthorizationService authorizationService;
    private final AdminProviderService providerService;
    private final AdminGoodsService goodsService;
    private final AdminGoodsDataService goodsDataService;
    private final FileService fileService;
    private final CustomerCompanyRepository customerCompanyRepository;

    private static final List<String> ALLOWED_EXTENSIONS_STRUCTURED = Arrays.asList("xlsx", "xls", "csv");
    private static final List<String> ALLOWED_MIME_STRUCTURED = Arrays.asList("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel", "text/csv", "application/haansoftxlsx");
    private static final List<String> ALLOWED_EXTENSIONS_UNSTRUCTURED = Arrays.asList(
            "txt", "csv", "pdf", "doc", "docx", "xls", "xlsb", "xlsm", "xlsx", "ppt", "pbix", "pbit",
            "xltx", "pptx");

    /*********************************************************************************************************************
    *
    * Provider 관리 API
    *
    *********************************************************************************************************************/
    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
    })
    @Operation(summary = "데이타 프로바이더 목록 조회 - with pagination", description = "데이타 프로바이더 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/provider")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<PaginationRes<ProviderRes>> getAllProviders(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(providerService.getAllProviders(companyNo, role, page, size));
    }

    @ApiOrder(2)
    @Operation(summary = "데이타 프로바이더명 목록 조회 - without pagination", description = "데이타 프로바이더명 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/provider/name")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<ProviderNameRes>> getAllProviderNm() {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(providerService.getAllProviderNm(companyNo, role));
    }

    @ApiOrder(3)
    @Operation(summary = "데이타 프로바이더 등록", description = "데이타 프로바이더를 등록합니다")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/provider")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_SUB_ADMIN})
    public BaseResponse<ProviderCreateRes> createProvider(
            @RequestBody @Valid ProviderCreateReq body) {

        return BaseResponseFactory.create(providerService.createProvider(body));
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더번호")
    })
    @Operation(summary = "데이타 프로바이더 상세 조회", description = "데이타 프로바이더 1개를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<ProviderDetailRes> getProvider(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(providerService.getProvider(companyNo, role, providerNo));
    }

    @ApiOrder(5)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더번호")
    })
    @Operation(summary = "데이타 프로바이더 수정", description = "데이타 프로바이더를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/providers/{providerNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<String> updateProvider(
            @RequestBody @Valid ProviderUpdateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

        providerService.updateProvider(providerNo, body);

        return BaseResponseFactory.create("success");
    }

    // @ApiOrder(6)
    // @Parameters({
    // @Parameter(name = "providerNo", description = "데이타 프로바이더번호")
    // })
    // @Operation(summary = "데이타 프로바이더 삭제", description = "데이타 프로바이더를 삭제합니다.")
    // @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    // @DeleteMapping("/providers/{providerNo}")
    // @RequirePermission(roles = { Role.ADMIN })
    // public BaseResponse<String> deleteProvider(
    // @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

    // providerService.deleteProvider(providerNo);

    // return BaseResponseFactory.create("success");
    // }

    @ApiOrder(7)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더번호")
    })
    @Operation(summary = "데이타 프로바이더 최신 수정일, 수정자", description = "데이타 프로바이더의 최신 수정일과 수정자를 반납합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/latest-updated")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_PLUS_OPERATOR})
    public BaseResponse<ProviderLatestUpdatedRes> getProviderLatestUpdated(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(providerService.getProviderLatestUpdated(providerNo));
    }

    /*********************************************************************************************************************
    *
    * Goods 관리 API
    *
    *********************************************************************************************************************/

    @ApiOrder(8)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더번호")
    })
    @Operation(summary = "상품 목록 조회 - without pagination", description = "상품 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/name")
    public BaseResponse<List<GoodsLiteRes>> getGoodsNames(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(goodsService.getAllGoodsNames(providerNo));
    }

    @ApiOrder(8)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "keyword", description = "상품명 또는 상품설명 검색"),
            @Parameter(name = "providerNo", description = "데이타 프로바이더번호")
    })
    @Operation(summary = "상품 목록 조회 - with pagination", description = "상품 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<PaginationRes<GoodsRes>> getAllGoods(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsService.getAllGoods(companyNo, role, page, size, keyword, providerNo));
    }

    @ApiOrder(9)
    @Operation(summary = "상품 등록", description = "상품을 등록합니다")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/providers/{providerNo}/goods")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_SUB_ADMIN})
    public BaseResponse<GoodsCreateRes> createGoods(
            @RequestBody @Valid GoodsCreateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsService.createGoods(role, providerNo, body));
    }

    @ApiOrder(10)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품 번호")
    })
    @Operation(summary = "상품 상세 조회", description = "상품 1개를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN})
    public BaseResponse<GoodsDetailRes> getGoodsDetail(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsService.getGoods(companyNo, role, providerNo, goodsNo));
    }

    @ApiOrder(11)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품 번호")
    })
    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/providers/{providerNo}/goods/{goodsNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_SUB_ADMIN})
    public BaseResponse<String> updateGoods(
            @RequestBody @Valid GoodsUpdateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        goodsService.updateGoods(companyNo, role, providerNo, goodsNo, body);

        return BaseResponseFactory.create("success");
    }

    // @ApiOrder(12)
    // @Parameters({
    // @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
    // @Parameter(name = "goodsNo", description = "상품 번호")
    // })
    // @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    // @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    // @DeleteMapping("/providers/{providerNo}/goods/{goodsNo}")
    // @RequirePermission(roles = { Role.ADMIN })
    // public BaseResponse<String> deleteGoods(
    // @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
    // @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

    // goodsService.deleteGoods(goodsNo);

    // return BaseResponseFactory.create("success");
    // }

    @ApiOrder(13)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품 번호")
    })
    @Operation(summary = "상품 최신 수정일, 수정자", description = "상품의 최신 수정일과 수정자를 반납합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/latest-updated")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN})
    public BaseResponse<GoodsLatestUpdatedRes> getGoodsLatestUpdated(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(goodsService.getGoodsLatestUpdated(goodsNo));
    }

    /*********************************************************************************************************************
     *
     * Goods Data 관리 API
     *
     *********************************************************************************************************************/

    @ApiOrder(14)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "데이타 목록 조회 - withpagination", description = "데이터 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/goods-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<PaginationRes<GoodsDataRes>> getGoodsDataSoueces(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsDataService.getAllGoodsData(companyNo, role, page, size, keyword, providerNo, goodsNo));
    }

    // [2025-05-09] 권한 상세 목록 조회 API 추가
    @ApiOrder(15)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
            @Parameter(name = "providerNo", description = "프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품 번호"),
            @Parameter(name = "companyNm", description = "검색할 고객사 명 (like search)"),
    })
    @Operation(summary = "권한 상세 목록 조회 API (with pagination)", description = "해당 상품을 구독하고 있는 회사 목록과 매핑된 권한 내역을 조회합니다. <br>페이지네이션 처리되어 'companyNm' 필드를 기준으로 오름차순(ASC) 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/company-permissions")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<PaginationRes<GoodsCompanyPermissionRes>> getCompanyPermissions(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyNm", required = false) String companyNm,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(goodsDataService.getCompanyPermissions(page, size, providerNo, goodsNo, companyNm));
    }

    @ApiOrder(15)
    @Operation(summary = "문서 타입 카테고리 조회", description = "문서 타입 카테고리를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/category/doc")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<GoodsDataCategoryRes>> getGoodsDataDocCategorys() {

        return BaseResponseFactory.create(goodsDataService.getDocTypeCategorys());
    }

    @ApiOrder(16)
    @Operation(summary = "테마 타입 카테고리 조회", description = "테마 타입 카테고리를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/category/theme")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<GoodsDataCategoryRes>> getGoodsDataThemeCategorys() {

        return BaseResponseFactory.create(goodsDataService.getThemeTypeCategorys());
    }

    @ApiOrder(17)
    @Operation(summary = "언어 타입 카테고리 조회", description = "언어 타입 카테고리를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/category/lang")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<GoodsDataCategoryRes>> getGoodsDataSubCategorys() {

        return BaseResponseFactory.create(goodsDataService.getLangTypeCategorys());
    }

    @ApiOrder(18)
    @Operation(summary = "정형 데이타 타입 목록을 조회", description = "정형 데이타 타입 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/structured-data-type")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR})
    public BaseResponse<List<ComponentInfoRes>> getStructuredDataType() {

        return BaseResponseFactory.create(goodsDataService.getStructuredDataType());
    }

    @ApiOrder(19)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호")
    })
    @Operation(summary = "데이터명 목록 조회", description = "데이터명 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/goods-data/name")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<GoodsDataNameRes>> getAllGoodsDataNm(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsDataService.getAllGoodsDataNm(companyNo, role, providerNo, goodsNo));
    }

    @ApiOrder(20)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호"),
            @Parameter(name = "goodsDataNo", description = "상품데이타번호")
    })
    @Operation(summary = "데이터 상세 조회", description = "데이터 싱세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/goods-datas/{goodsDataNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_SUB_ADMIN, Role.OPERATOR, Role.API_PLUS_OPERATOR})
    public BaseResponse<GoodsDataDetailRes> getGoodsData(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo,
            @PathVariable(value = "goodsDataNo") @Min(value = 1) Integer goodsDataNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsDataService.getGoodsData(companyNo, role, providerNo, goodsNo, goodsDataNo));
    }

    @ApiOrder(21)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호"),
    })
    @Operation(summary = "데이터 등록: 신규등록", description = "데이터 등록: 신규 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/providers/{providerNo}/goods/{goodsNo}/goods-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<String> createGoodsData(
            @RequestBody @Valid GoodsDataCreateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        goodsDataService.createGoodsData(companyNo, role, providerNo, goodsNo, body);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(22)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호"),
            @Parameter(name = "goodsNo", description = "상품번호"),
            @Parameter(name = "goodsDataNo", description = "상품데이타번호")
    })
    @Operation(summary = "데이터 등록: 기존 데이타 업데이트", description = "데이터 등록: 기존 데이타 업데이트합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/providers/{providerNo}/goods/{goodsNo}/goods-datas/{goodsDataNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<String> updateGoodsData(
            @RequestBody @Valid GoodsDataUpdateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo,
            @PathVariable(value = "goodsDataNo") @Min(value = 1) Integer goodsDataNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        goodsDataService.updateGoodsData(companyNo, role, providerNo, goodsNo, goodsDataNo, body);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(91)
    @Operation(summary = "정형 데이타 파일 업로드", description = "정형 데이타 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/goods-datas/upload-file/structured-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<GoodsDataFileUploadRes> uploadStructuredDataFile(
            @RequestPart(value = "file", required = true) MultipartFile file,
            @Schema(implementation = StructuredDataFileReq.class) @RequestPart(value = "data", required = true) @Valid StructuredDataFileReq structuredDataFileReq)
            throws IOException {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));
        String companyCd = customerCompany.getCustomerCompanyCd();

        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS_STRUCTURED.contains(extension)) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        // String mimeType = file.getContentType();
        // if (!ALLOWED_MIME_STRUCTURED.contains(mimeType)) {
        // throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        // }

        return BaseResponseFactory.create(goodsDataService.uploadStructuredDataFile(companyCd, role, file, false, structuredDataFileReq));
    }

    @ApiOrder(92)
    @Operation(summary = "비정형 데이타 파일 업로드", description = "비정형 데이타 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/goods-datas/upload-file/unstructured-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<FileUploadRes> uploadUnstructuredDataFile(
            @RequestParam(value = "fileHash", required = false, defaultValue = "") String fileHash,
            @RequestPart(value = "file", required = true) MultipartFile file) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));
        String companyCd = customerCompany.getCustomerCompanyCd();

        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS_UNSTRUCTURED.contains(extension)) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        return BaseResponseFactory.create(fileService.uploadFile(companyCd, role, file, false, fileHash));
    }

    @ApiOrder(101)
    @Parameters({
            @Parameter(name = "fileName", required = true, description = "파일명"),
            @Parameter(name = "fileHash", required = true, description = "파일해시값"),
            @Parameter(name = "goodsDataNm", required = true, description = "상품데이타명")
    })
    @Operation(summary = "동일한 file 존재 여부 체크", description = "")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping(value = "/goods-datas/upload-file/unstructured-data/exist")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<FileExistRes>> fileExists(
            @RequestParam(value = "fileName", required = true, defaultValue = "") String fileName,
            @RequestParam(value = "fileHash", required = true, defaultValue = "") String fileHash,
            @RequestParam(value = "goodsDataNm", required = true, defaultValue = "") String goodsDataNm) {

        if (fileName == null || fileName.isEmpty() || fileHash == null || fileHash.isEmpty() || goodsDataNm == null || goodsDataNm.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }

        List<FileExistRes> result = new ArrayList<>();

        List<GoodsData> goodsDatas = goodsDataService.getGoodsData(goodsDataNm);
        if (goodsDatas.size() > 0) {
            List<UploadedFile> uploadedFiles = fileService.fileExist(fileName, fileHash);
            if (uploadedFiles.size() > 0) {
                for (GoodsData goodsData : goodsDatas) {
                    for (UploadedFile uploadedFile : uploadedFiles) {
                        if (goodsData.getGoodsDataNo() != null && uploadedFile.getFileRelationshipObjectDistinguishNo() != null
                                && goodsData.getGoodsDataNo().equals(uploadedFile.getFileRelationshipObjectDistinguishNo())) {
                            result.add(new FileExistRes(goodsData.getGoodsDataNo(), uploadedFile.getFileNo()));
                        }
                    }
                }
            }
        }

        return BaseResponseFactory.create(result);
    }

    @ApiOrder(99)
    @Operation(summary = "비정형 Meta 파일 업로드", description = "비정형 Meta 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/axplatform/upload")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<String> uploadMetaFile(
            @RequestParam(value = "goodsDataNo", required = true, defaultValue = "") Integer goodsDataNo) {

        goodsDataService.uploadMetaFile(goodsDataNo);

        return BaseResponseFactory.create("success");
    }
}
