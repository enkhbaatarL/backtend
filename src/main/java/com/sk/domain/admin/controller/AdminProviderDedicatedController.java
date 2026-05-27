package com.sk.domain.admin.controller;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.*;
import com.sk.domain.admin.dto.response.file.FileExistRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.dto.response.file.GoodsDataFileUploadRes;
import com.sk.domain.admin.dto.response.provider.*;
import com.sk.domain.admin.service.AdminGoodsDataService;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Tag(name = "03. 데이타 프로바이더 관리 API", description = "데이타 프로바이더 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/provider-management")
@RequiredArgsConstructor
public class AdminProviderDedicatedController {
    private final AuthorizationService authorizationService;
    private final AdminGoodsDataService goodsDataService;
    private final FileService fileService;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;

    private static final List<String> ALLOWED_EXTENSIONS_STRUCTURED = Arrays.asList("xlsx", "xls", "csv");

    private static final List<String> ALLOWED_EXTENSIONS_UNSTRUCTURED = Arrays.asList(
            "txt", "csv", "pdf", "doc", "docx", "xls", "xlsb", "xlsm", "xlsx", "ppt", "pbix", "pbit",
            "xltx", "pptx", "eml", "md");


    @ApiOrder(14)
    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
        @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
        @Parameter(name = "sortDirection", description = "데이터 정렬 방향"),
        @Parameter(name = "sortProperty", description = "데이타 정렬 조건"),
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
        @RequestParam(value = "sortDirection", required = false) String sortDirection,
        @RequestParam(value = "sortProperty", required = false) String sortProperty,
        @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
        @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();

        return BaseResponseFactory.create(goodsDataService.getAllGoodsData(companyNo, role, page, size,
            keyword, sortDirection, sortProperty, providerNo, goodsNo));
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

        Role role = authorizationService.getCurrentRole();
        int companyNo = authorizationService.getCompanyNo();

        List<GoodsData> goodsDatas = goodsDataService.getGoodsData(goodsDataNm);
        if (goodsDatas.size() > 0) {
            List<UploadedFile> uploadedFiles = fileService.fileExist(fileName, fileHash);
            if (uploadedFiles.size() > 0) {
                for (GoodsData goodsData : goodsDatas) {
                    boolean isTarget = false;
                    if (role == Role.ADMIN || role == Role.API_SUB_ADMIN) {
                        if (goodsData.getGoods() != null && Boolean.TRUE.equals(goodsData.getGoods().getIsPublic())) {
                            isTarget = true;
                        }
                    } else if (role == Role.OPERATOR || role == Role.API_PLUS_OPERATOR) {
                        if (goodsData.getGoods() != null && Boolean.FALSE.equals(goodsData.getGoods().getIsPublic())
                            && customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsData.getGoods().getGoodsNo()).isPresent()) {
                            isTarget = true;
                        }
                    }

                    if (isTarget) {
                        for (UploadedFile uploadedFile : uploadedFiles) {
                            if (goodsData.getGoodsDataNo() != null && uploadedFile.getFileRelationshipObjectDistinguishNo() != null
                                && goodsData.getGoodsDataNo().equals(uploadedFile.getFileRelationshipObjectDistinguishNo())) {
                                result.add(new FileExistRes(goodsData.getGoodsDataNo(), uploadedFile.getFileNo()));
                            }
                        }
                    }
                }
            }
        }

        return BaseResponseFactory.create(result);
    }

    @AccessIpCheck()
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

    @AccessIpCheck()
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

    @ApiOrder(93)
    @Operation(summary = "정형 데이타 Json 업로드", description = "정형 Json 데이터를 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/goods-datas/upload-temp/structured-data", consumes = MediaType.APPLICATION_JSON_VALUE)
    //@RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<GoodsDataJsonUploadRes> uploadStructuredDataFile(
        @RequestBody StructuredDataJsonReq structuredDataJsonReq){

        /** TODO 인증추가 필요
        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
         */
        int companyNo = 2013;
        Role role = Role.OPERATOR;

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(
                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));
        String companyCd = customerCompany.getCustomerCompanyCd();

        if (structuredDataJsonReq == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }
        return BaseResponseFactory.create(goodsDataService.uploadStructuredDataJson(companyCd, role, structuredDataJsonReq));
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


    @ApiOrder(99)
    @Operation(summary = "비정형 Meta 파일 업로드", description = "비정형 Meta 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/axplatform/upload")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<String> uploadMetaFile(
        @RequestParam(value = "goodsDataNo", required = false) Integer goodsDataNo,
        @RequestParam(value = "goodsNo", required = false) Integer goodsNo) {

        if (goodsDataNo != null) {
            goodsDataService.uploadUnstructuredDataToVector(goodsDataNo);
        } else if (goodsNo != null) {
            goodsDataService.uploadUnstructuredDataToVectorByGoodsNo(goodsNo);
        } else {
            throw AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER, "goodsDataNo or goodsNo is required");
        }

        return BaseResponseFactory.create("success");
    }

}
