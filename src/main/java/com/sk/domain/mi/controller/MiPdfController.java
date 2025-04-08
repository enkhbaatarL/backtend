package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "10. PDF viewer API", description = "PDF viewer API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/pdf-viewer")
@RequiredArgsConstructor
public class MiPdfController {

    private final MiPdfService pdfService;

    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
    })
    @Operation(summary = "PDF 메타 데이터 조회 API", description = "PDF 메타 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/files/{fileNo}/metadata")
    public BaseResponse<PdfMetadataRes> getPdfMetadata(
        @PathVariable(value = "fileNo") Integer fileNo
    ) {
        return BaseResponseFactory.create(pdfService.getPdfMetadata(fileNo));
    }

}
