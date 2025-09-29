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

    private static final List<String> ALLOWED_EXTENSIONS_STRUCTURED = Arrays.asList("xlsx", "xls", "csv");

    private static final List<String> ALLOWED_EXTENSIONS_UNSTRUCTURED = Arrays.asList(
            "txt", "csv", "pdf", "doc", "docx", "xls", "xlsb", "xlsm", "xlsx", "ppt", "pbix", "pbit",
            "xltx", "pptx");

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
}
