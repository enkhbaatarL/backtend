package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.mi.service.MiPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "10. PDF viewer API", description = "PDF viewer API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/pdf-viewer")
@RequiredArgsConstructor
public class MiPdfViewerController {

    private final MiPdfService pdfService;

    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "fileNo", description = "파일 번호"),
    })
    @Operation(summary = "PDF 메타 데이터 조회 API", description = "PDF 메타 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/files/{fileNo}/metadata")
    public BaseResponse<PdfMetadataRes> getPdfFileMetadata(
        @PathVariable(value = "fileNo") Long fileNo
    ) {
        return BaseResponseFactory.create(pdfService.getPdfFileMetadata(fileNo));
    }

    @ApiOrder(2)
    @Parameters({
        @Parameter(name = "fileNo", description = "파일 번호"),
        @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
    })
    @Operation(summary = "PDF 이미지 페이지 조회 API", description = "PDF 이미지 페이지를 한장씩 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/files/{fileNo}/images")
    public ResponseEntity<byte[]> getImagePageFromPdf(
        @PathVariable(value = "fileNo") Long fileNo,
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page
    ) {
        byte[] imageBytes = pdfService.getImagePageFromPdf(fileNo, page);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

}
