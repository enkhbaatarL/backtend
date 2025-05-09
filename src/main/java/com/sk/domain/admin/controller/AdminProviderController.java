package com.sk.domain.admin.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.*;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.dto.response.file.GoodsDataFileUploadRes;
import com.sk.domain.admin.dto.response.provider.*;
import com.sk.domain.admin.service.AdminFileService;
import com.sk.domain.admin.service.AdminGoodsDataService;
import com.sk.domain.admin.service.AdminGoodsService;
import com.sk.domain.admin.service.AdminProviderService;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final AdminFileService adminFileService;
    private final CustomerCompanyRepository customerCompanyRepository;

    private static final List<String> ALLOWED_EXTENSIONS_STRUCTURED = Arrays.asList("xlsx", "xls", "csv");
    private static final List<String> ALLOWED_MIME_STRUCTURED = Arrays.asList("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel", "text/csv");
    private static final List<String> ALLOWED_EXTENSIONS_UNSTRUCTURED = Arrays.asList("txt", "csv", "pdf", "doc", "docx", "xls", "xlsx", "ppt",
            "pptx");
    private static final long MAX_SIZE = 24 * 1024 * 1024;

    // ADMIN-SKCC
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0NDMDAwQFNLQ0MuY29tIiwiaWF0IjoxNzQ0Mjc0ODcxLCJ1c2VyTm8iOjEwMDAwNDQsImNvbXBhbnlObyI6MjAxMiwiY29tcGFueUNkIjoiU0tDQyIsInJvbGUiOiJBRE1JTiIsImV4cCI6MTc4MDI3NDg3MX0.hVm8KPlcTPwjxGtdRr8VH1uIrGFIfIePb_FyVkiquHc

    // OPERATOR-SKHY
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0hZMDAwQFNLSFkuY29tIiwiaWF0IjoxNzQ0Mjc0OTA0LCJ1c2VyTm8iOjEwMDAwNDUsImNvbXBhbnlObyI6MjAxNSwiY29tcGFueUNkIjoiU0tIWSIsInJvbGUiOiJPUEVSQVRPUiIsImV4cCI6MTc4MDI3NDkwNH0.T0JZ5XJva9dokRGhDhipWTvlRi92kSO9cUEzpFfXgSk

    // OPERATOR-SKGAS
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0dBUzAwMEBTS0dBUy5jb20iLCJpYXQiOjE3NDQyNzQ5MjcsInVzZXJObyI6MTAwMDA1MSwiY29tcGFueU5vIjoyMDE0LCJjb21wYW55Q2QiOiJTS0dBUyIsInJvbGUiOiJPUEVSQVRPUiIsImV4cCI6MTc4MDI3NDkyN30.p3BWi9foUIVa2LoG4GF6yBLvyoV-1ZTb7Q2S_JS2U1I

    // USER-SKHY
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0hZMDAxQFNLSFkuY29tIiwiaWF0IjoxNzQ0ODUyODE4LCJ1c2VyTm8iOjEwMDAwNTMsImNvbXBhbnlObyI6MjAxNSwiY29tcGFueUNkIjoiU0tIWSIsInJvbGUiOiJVU0VSIiwiaXNPdGhlclNlcnZpY2VBY2Nlc3NQb3NzaWJsZSI6ZmFsc2UsImV4cCI6MTc4MDg1MjgxOH0.QVEABqTE7DIwcjRP81FvxKCmThW3ImwveJKFecaiXcA

    // USER-SKGAS
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0dBUzAwMUBTS0dBUy5jb20iLCJpYXQiOjE3NDQ4NTMwMTUsInVzZXJObyI6MTAwMDA4NSwiY29tcGFueU5vIjoyMDE0LCJjb21wYW55Q2QiOiJTS0dBUyIsInJvbGUiOiJVU0VSIiwiaXNPdGhlclNlcnZpY2VBY2Nlc3NQb3NzaWJsZSI6ZmFsc2UsImV4cCI6MTc4MDg1MzAxNX0.WcDDwXHwvXtkSITUeLE18AzFgWkwSCEEvGA_tqmx5X8

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
        return BaseResponseFactory.create(goodsService.getGoodsNames(providerNo));
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

    // [2025-05-09] 상품데이타 목록 조회 API 추가
    @ApiOrder(15)
    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
        @Parameter(name = "providerNo", description = "프로바이더 번호"),
        @Parameter(name = "goodsNo", description = "상품 번호")
    })
    @Operation(summary = "권한 상세 목록 조회 API (with pagination)", description = "상품을 구독하고 있는 회사의 매핑된 권한 내역을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/company-permissions")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<PaginationRes<GoodsCompanyPermissionRes>> getCompanyPermissions(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo
    ) {
        return BaseResponseFactory.create(goodsDataService.getCompanyPermissions(page, size, providerNo, goodsNo));
    }

    @ApiOrder(15)
    @Operation(summary = "문서 타입 카테고리 조회", description = "문서 타입 카테고리를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/category/doc")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<GoodsDataCategoryRes>> getGoodsDataMainCategorys() {

        return BaseResponseFactory.create(goodsDataService.getDocTypeCategorys());
    }

    @ApiOrder(16)
    @Operation(summary = "언어 타입 카테고리 조회", description = "언어 타입 카테고리를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/category/lang")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<List<GoodsDataCategoryRes>> getGoodsDataSubCategorys() {

        return BaseResponseFactory.create(goodsDataService.getLangTypeCategorys());
    }

    @ApiOrder(17)
    @Operation(summary = "정형 데이타 타입 목록을 조회", description = "정형 데이타 타입 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/structured-data-type")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR})
    public BaseResponse<List<ComponentInfoRes>> getStructuredDataType() {

        return BaseResponseFactory.create(goodsDataService.getStructuredDataType());
    }

    @ApiOrder(18)
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

    @ApiOrder(19)
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

    @ApiOrder(20)
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

    @ApiOrder(21)
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
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Schema(implementation = StructuredDataFileReq.class) @RequestPart(value = "data", required = true) @Valid StructuredDataFileReq structuredDataFileReq)
            throws IOException {

        int userNo = authorizationService.getUserNo();
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

        String mimeType = file.getContentType();
        if (!ALLOWED_MIME_STRUCTURED.contains(mimeType)) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        if (file.getSize() > MAX_SIZE) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        return BaseResponseFactory.create(goodsDataService.uploadStructuredDataFile(userNo, companyCd, role, file, false, structuredDataFileReq));
    }

    @ApiOrder(92)
    @Operation(summary = "비정형 데이타 파일 업로드", description = "비정형 데이타 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/goods-datas/upload-file/unstructured-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<FileUploadRes> uploadUnstructuredDataFile(
            @RequestPart(value = "file", required = false) MultipartFile file) {

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

        if (file.getSize() > MAX_SIZE) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        return BaseResponseFactory.create(adminFileService.uploadFile(companyCd, role, file, false));
    }

    @ApiOrder(93)
    @Operation(summary = "비정형 데이타 파일 다운로드", description = "비정형 데이타 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/goods-datas/download-file/unstructured-data/{fileNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public ResponseEntity<byte[]> downloadFile(
            @RequestBody @Valid FileDownloadReq body,
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo)
            throws UnsupportedEncodingException {

        UploadedFile uploadedFile = adminFileService.getFile(fileNo);

        // 검증 로직
        if (uploadedFile.getFileRelationshipObjectSeparateCd() != FileRelationshipObjectSeparateCode.GUDAT.getCode()) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }
        if (uploadedFile.getFileNo() != body.fileNo()) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }
        if (!uploadedFile.getOriginalFileNm().equals(body.fileNm())) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }

        FileDownloadRes fileDownloadRes = adminFileService.downloadFile(fileNo);

        String encodedFileName = URLEncoder.encode(fileDownloadRes.fileNm(), StandardCharsets.UTF_8.toString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileDownloadRes.fileData().length);
        httpHeaders.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
    }
}
