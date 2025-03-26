package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.service.AdminFileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "09. 파일 관리 API", description = "파일 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/file-manangement")
@RequiredArgsConstructor
public class AdminFileController {
    private final AdminFileService adminFileService;

    @ApiOrder(1)
    @Operation(summary = "파일 업로드", description = "파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadRes> uploadFile(
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return BaseResponseFactory.create(adminFileService.uploadFile(file));
    }

    @ApiOrder(2)
    @Operation(summary = "파일 다운로드", description = "파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/{fileNo}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo) {

        FileDownloadRes fileDownloadRes = adminFileService.downloadFile(fileNo);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileDownloadRes.fileData().length);
        httpHeaders.setContentDispositionFormData("attachment", fileDownloadRes.fileNm());

        return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
    }

    @ApiOrder(3)
    @Operation(summary = "파일 삭제", description = "파일을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/{fileNo}")
    public BaseResponse<String> deleteFile(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo) {

        adminFileService.deleteFile(fileNo);

        return BaseResponseFactory.create("success");
    }

    public BaseResponse<String> updateFile(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo,
            @PathVariable(value = "fileRelationshipObjectDistinguishNo") @Min(value = 1) Integer fileRelationshipObjectDistinguishNo,
            @PathVariable(value = "fileRelationshipObjectSeparateCode") String fileRelationshipObjectSeparateCode) {

        adminFileService.updateFile(fileNo, fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(4)
    @Operation(summary = "presigned URL 생성", description = "presigned URL 생성합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/presigned-url/{fileNo}")
    public BaseResponse<String> getPresignedUrl(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo) {

        return BaseResponseFactory.create(adminFileService.getPresignedUrl(fileNo));
    }

    @ApiOrder(5)
    @Operation(summary = "파일 목록 조회", description = "파일 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/all")
    public BaseResponse<List<String>> getAll() {

        return BaseResponseFactory.create(adminFileService.getAll());
    }
}
