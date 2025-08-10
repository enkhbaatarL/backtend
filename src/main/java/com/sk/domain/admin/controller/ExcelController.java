package com.sk.domain.admin.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.request.ExcelReq;
import com.sk.domain.admin.service.ExcelService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import org.hibernate.annotations.processing.Exclude;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Profile("!dedicated")
@Tag(name = "00. Excel API", description = "Excel API 입니다.")
@Slf4j
@Hidden
@RestController
@RequestMapping("/api/v1/admin/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelService excelService;

    // [
    // [
    // { "name": "홍길동", "grade": "A" },
    // { "name": "이몽룡", "grade": "B" }
    // ],
    // [
    // { "name": "홍길동", "grade": "C" },
    // { "name": "이몽룡", "grade": "D" }
    // ]
    // ]

    @Operation(summary = "Excel download API", description = "Excel 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/download-excel")
    public ResponseEntity<ByteArrayResource> downloadExcelFile(HttpServletResponse response,
            @RequestBody List<List<ExcelReq>> datas) throws IOException {

        datas.forEach(
                list -> list.forEach(data -> log.info("Name: " + data.getName() + ", Grade: " + data.getGrade())));

        ByteArrayResource excelResource = excelService.downloadExcelFile(response, datas);

        return ResponseEntity.ok()
                .contentLength(excelResource.contentLength())
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelResource);
    }

    @Operation(summary = "Excel upload API", description = "Excel 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> uploadExcelFile(@RequestParam("file") MultipartFile file) throws IOException {

        if (file != null) {
            log.info("ok - file received: " + file.getOriginalFilename());
        } else {
            log.info("null");
        }

        excelService.uploadExcelFile(file);

        return BaseResponseFactory.create("success");
    }

}
