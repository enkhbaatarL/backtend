package com.sk.domain.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.service.AdminFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
  @Operation(summary = "파일 업로드", description = "파일을 업로드합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<String> uploadFile(
      @RequestPart(value = "file", required = false) MultipartFile file) {

    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(2)
  @Operation(summary = "파일 다운로드", description = "파일을 다운로드합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<String> downloadFile(HttpServletResponse response) {

    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(2)
  @Operation(summary = "파일 삭제", description = "파일을 삭제합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<String> deleteFile(Integer fileId) {

    return BaseResponseFactory.create("todo");
  }

  
  // 파일 링크
}
