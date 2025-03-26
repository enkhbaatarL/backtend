package com.sk.domain.admin.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.service.AdminFileService;
import com.sk.domain.admin.service.AdminProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "03. 데이타 프로바이더 관리 API", description = "데이타 프로바이더 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/provider-manangement")
@RequiredArgsConstructor
public class AdminProviderController {
    private final AdminProviderService providerService;
    private final AdminFileService adminFileService;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("csv", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final long MAX_SIZE = 24 * 1024 * 1024;

    @ApiOrder(1)
    @Operation(summary = "데이타 프로바이더 목록 조회 - without pagination", description = "데이타 프로바이더 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/provider/all")
    public BaseResponse<List<ProviderNameRes>> getAllProviders() {

        return BaseResponseFactory.create(providerService.getAllProviders());
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
    })
    @Operation(summary = "데이타 프로바이더 목록 조회 - with pagination", description = "데이타 프로바이더 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/provider")
    public BaseResponse<PaginationRes<ProviderRes>> getAllProviders(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size) {

        return BaseResponseFactory.create(providerService.getAllProviders(page, size));
    }

    @ApiOrder(3)
    @Operation(summary = "데이타 프로바이더 등록", description = "데이타 프로바이더를 등록합니다")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/provider")
    public BaseResponse<String> createProvider(
            @RequestBody @Valid ProviderCreateReq body) {

        providerService.createProvider(body);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
    })
    @Operation(summary = "데이타 프로바이더 상세 조회", description = "데이타 프로바이더 1개를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}")
    public BaseResponse<ProviderDetailRes> getPrivider(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(providerService.getPrivider(providerNo));
    }

    @ApiOrder(5)
    @Parameters({
            @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
    })
    @Operation(summary = "데이타 프로바이더 수정", description = "데이타 프로바이더를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/providers/{providerNo}")
    public BaseResponse<String> updateProvider(
            @RequestBody @Valid ProviderUpdateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

        providerService.updateProvider(body, providerNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(6)
    @Parameters({
            @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
    })
    @Operation(summary = "데이타 프로바이더 삭제", description = "데이타 프로바이더를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/providers/{providerNo}")
    public BaseResponse<String> deleteProvider(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {

        providerService.deleteProvider(providerNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(7)
    @Parameters({
            @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
    })
    @Operation(summary = "데이타 프로바이더 최신 수정일, 수정자", description = "데이타 프로바이더의 최신 수정일과 수정자를 반납합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/latest-updated/")
    public BaseResponse<ProviderLatestUpdatedRes> getProviderLatestUpdated(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(providerService.getProviderLatestUpdated(providerNo));
    }

    @ApiOrder(8)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "keyword", description = "상품명 또는 상품설명 검색"),
            @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
    })
    @Operation(summary = "상품 목록 조회 - with pagination", description = "상품 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods")
    public BaseResponse<PaginationRes<GoodsRes>> getAllGoods(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
        return BaseResponseFactory.create(providerService.getAllGoods(page, size, keyword, providerNo));
    }

    @ApiOrder(9)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
            @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
    })
    @Operation(summary = "상품 상세 조회", description = "상품 1개를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}")
    public BaseResponse<GoodsDetailRes> getGoods(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(providerService.getGoods(providerNo, goodsNo));
    }

    @ApiOrder(10)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
            @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
    })
    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/providers/{providerNo}/goods/{goodsNo}")
    public BaseResponse<String> updateGoods(
            @RequestBody @Valid GoodsUpdateReq body,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        providerService.updateGoods(providerNo, goodsNo, body);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(11)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
            @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
    })
    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/providers/{providerNo}/goods/{goodsNo}")
    public BaseResponse<String> deleteGoods(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        providerService.deleteGoods(providerNo, goodsNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(12)
    @Parameters({
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
            @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
    })
    @Operation(summary = "상품 최신 수정일, 수정자", description = "상품의 최신 수정일과 수정자를 반납합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/latest-updated/")
    public BaseResponse<GoodsLatestUpdatedRes> getGoodsLatestUpdated(
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
        return BaseResponseFactory.create(providerService.getGoodsLatestUpdated(providerNo, goodsNo));
    }

    @ApiOrder(13)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
            @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
            @Parameter(name = "goodsNo", description = "상품번호 (기본값: 70000)")
    })
    @Operation(summary = "데이타 목록 조회 - withpagination", description = "데이터 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/providers/{providerNo}/goods/{goodsNo}/goodsdata")
    public BaseResponse<PaginationRes<GoodsDataRes>> getGoodsDataSoueces(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
            @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

        return BaseResponseFactory.create(providerService.getAllGoodsData(page, size, keyword, providerNo, goodsNo));
    }

    // todo: 데이타 카테고리 조회(대분류, 중분류 같이 또는 따로?)

    // todo: 데이타 등록

    // todo: 데이타명 목록 조회
    // todo: 데이타 수정

    // 정형(Structured), 비정형(Unstructured) 업로드

    @ApiOrder(98)
    @Operation(summary = "데이타의 정형, 비정형 데이타 파일 업로드", description = "데이타의 정형, 비정형 데이타 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadRes> uploadFile(
            @RequestPart(value = "file", required = false) MultipartFile file) {

        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        if (file.getSize() > MAX_SIZE) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        return BaseResponseFactory.create(adminFileService.uploadFile(file));
    }

    @ApiOrder(99)
    @Operation(summary = "첨부 파일 다운로드", description = "첨부 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/download-file/{fileNo}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo) {

        FileDownloadRes fileDownloadRes = adminFileService.downloadFile(fileNo);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileDownloadRes.fileData().length);
        httpHeaders.setContentDispositionFormData("attachment", fileDownloadRes.fileNm());

        return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
    }

}
