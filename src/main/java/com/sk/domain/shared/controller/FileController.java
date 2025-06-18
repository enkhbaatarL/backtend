package com.sk.domain.shared.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.file.FileUpdateReq;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.shared.dto.request.file.FileDownloadReq;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "10. File API", description = "파일 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/file-management")
@RequiredArgsConstructor
public class FileController {
    private final AuthorizationService authorizationService;
    private final FileService fileService;
    private final CustomerCompanyRepository customerCompanyRepository;

    @ApiOrder(1)
    @Operation(summary = "파일 목록 조회", description = "파일 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/all")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public BaseResponse<String> getAll() {

        fileService.getAll();

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(2)
    @Operation(summary = "파일 업로드", description = "파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public BaseResponse<FileUploadRes> uploadFile(
            @RequestPart(value = "file", required = false) MultipartFile file) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));
        String companyCd = customerCompany.getCustomerCompanyCd();

        return BaseResponseFactory.create(fileService.uploadFile(companyCd, role, file, true));
    }

    @ApiOrder(3)
    @Operation(summary = "1개 파일 다운로드", description = "1개의 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/download/file")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public ResponseEntity<byte[]> downloadFile(
            @RequestBody @Valid FileDownloadReq body) throws UnsupportedEncodingException {

        FileDownloadRes fileDownloadRes = fileService.downloadFile(body);

        if (!fileDownloadRes.fileNm().equals(fileDownloadRes.fileNm())) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }

        if (!fileDownloadRes.fileSize().equals(fileDownloadRes.fileSize())) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }

        String encodedFileName = URLEncoder.encode(fileDownloadRes.fileNm(), StandardCharsets.UTF_8.toString()).replace("+", "%20");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileDownloadRes.fileData().length);
        httpHeaders.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
    }

    @ApiOrder(4)
    @Operation(summary = "1개 이상 파일 zip 압축 다운로드", description = "1개 이상의 파일을 zip으로 압축하여 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/download")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public ResponseEntity<byte[]> downloadFiles(
            @RequestParam(value = "prefix", required = true) String prefix,
            @RequestBody @Valid List<FileDownloadReq> body) throws UnsupportedEncodingException {

        byte[] fileData = fileService.downloadFiles(body);
        String encodedFileName = URLEncoder.encode(prefix.concat("_").concat(DateUtil.currentYYYYMMDDHHMMSS()).concat(".zip"),
                StandardCharsets.UTF_8.toString()).replace("+", "%20");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileData.length);
        httpHeaders.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<>(fileData, httpHeaders, HttpStatus.OK);
    }

    @ApiOrder(5)
    @Operation(summary = "파일 삭제", description = "파일을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/{fileNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public BaseResponse<String> deleteFile(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo) {

        fileService.deleteFile(fileNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(6)
    @Operation(summary = "파일 수정", description = "파일을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/{fileNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public BaseResponse<String> updateFile(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo,
            @RequestBody @Valid FileUpdateReq body) {

        fileService.updateFile(fileNo, body.fileRelationshipObjectDistinguishNo(), body.fileRelationshipObjectSeparateCode(),
                body.fileValidEndDt());

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(7)
    @Operation(summary = "1개 이상 파일 zip 압축 다운로드", description = "1개 이상의 파일을 zip으로 압축하여 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/direct-download")
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.USER})
    public ResponseEntity<byte[]> directDownloadFiles(
            @RequestParam(value = "prefix", required = true) String prefix,
            @RequestBody @Valid List<String> body) throws UnsupportedEncodingException {
        byte[] fileData = fileService.directDownloadFiles(body);
        String encodedFileName = URLEncoder.encode(prefix.concat("_").concat(DateUtil.currentYYYYMMDDHHMMSS()).concat(".zip"),
                StandardCharsets.UTF_8.toString()).replace("+", "%20");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileData.length);
        httpHeaders.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<>(fileData, httpHeaders, HttpStatus.OK);
    }

    @ApiOrder(99)
    @Operation(summary = "presigned URL 생성", description = "presigned URL 생성합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/presigned-url")
    public BaseResponse<String> getPresignedUrl(
            @RequestParam(value = "filePath", required = true) String filePath) {

        return BaseResponseFactory.create(fileService.getPresignedUrl(filePath));
    }

}
