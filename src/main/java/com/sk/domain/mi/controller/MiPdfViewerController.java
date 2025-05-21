package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.mi.service.MiPdfService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
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
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "fileNo", requireViewPermission = true
    )
    public BaseResponse<PdfMetadataRes> getPdfFileMetadata(@PathVariable(value = "fileNo") Integer fileNo) {
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
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "fileNo", requireViewPermission = true
    )
    public ResponseEntity<byte[]> getImagePageFromPdf(
        @PathVariable(value = "fileNo") Integer fileNo,
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @UserResolve User user
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        byte[] imageBytes = pdfService.getImagePageFromPdf(fileNo, page, user);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

}
