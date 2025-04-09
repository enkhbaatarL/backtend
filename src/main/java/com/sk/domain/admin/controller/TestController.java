package com.sk.domain.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.PDFUtil;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestLoginRes;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.admin.service.TestService;
import com.sk.domain.mi.service.MiPdfService;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.enums.UserRoleCd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "00. 테스트 샘플 API", description = "테스트 샘플 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final MiPdfService pdfService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    @Operation(summary = "사용자 정보 조회 API", description = "사용자 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/user-info")
    public BaseResponse<TestRes.GetUserInfo> getUserInfo(@RequestBody @Valid TestReq.getUserInfo testReq) {
        return BaseResponseFactory.create(testService.getUserInfo(testReq));
    }

    @Operation(summary = "테스트 유저 로그인", description = "테스트 유저 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/login/user")
    public BaseResponse<TestLoginRes> userLogin(@RequestBody @Valid TestReq.userLogin testLoginReq) {
        return BaseResponseFactory.create(testService.userLogin(testLoginReq));
    }

    @Operation(summary = "테스트 어드민 로그인", description = "테스트 어드민 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/login/admin")
    public BaseResponse<TestLoginRes> adminLogin(@RequestBody @Valid TestReq.adminLogin testLoginReq) {
        return BaseResponseFactory.create(testService.adminLogin(testLoginReq));
    }

    @Operation(summary = "토큰 검증", description = "토큰 검증을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/verify-token")
    public BaseResponse<Boolean> verifyToken(@RequestBody @Valid TestReq.verifyToken testVerifyTokenReq) {
        return BaseResponseFactory.create(testService.checkToken(testVerifyTokenReq));
    }

    @Operation(summary = "테스트(멀티테넌시 master mybatis) 알림 기록 조회", description = "테스트(멀티테넌시 master mybatis) 알림 기록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/sktelecom-email-access-log-mybatis")
    public BaseResponse<List<EmailAccessLog>> getSkTelecomEmailAccessLogByMybatis() {
        return BaseResponseFactory.create(testService.getSkTelecomEmailAccessLogByMybatis());
    }

    @Operation(summary = "테스트(멀티테넌시 secondary mybatis) 알림 기록 조회", description = "테스트(멀티테넌시 secondary mybatis) 알림 기록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/secondary-email-access-log-mybatis")
    public BaseResponse<List<EmailAccessLog>> getHynixEmailAccessLogByMybatis() {
        return BaseResponseFactory.create(testService.getHynixEmailAccessLogByMybatis());
    }

    @GetMapping("/token")
    public BaseResponse<String> getToken(@RequestParam("loginUserId") String loginUserId,
            @RequestParam("role") UserRoleCd role) {
        return BaseResponseFactory.create(testService.getToken(loginUserId, role));
    }

    @GetMapping("/delete/id")
    public BaseResponse<String> delete(@RequestParam("id") Integer id) {
        return BaseResponseFactory.create(testService.delete(id));
    }

    @PostMapping("/dynamic-query")
    public BaseResponse<List<Map<String, Object>>> getDynamicQuery(
            @RequestBody @Valid DynamicDataReq testReq) {
        return BaseResponseFactory.create(mybatisDynamicQueryService.getDynamicData(testReq));
    }

    // 테스트 코드
    @GetMapping(value = "/watermark", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getWatermarkedImage() throws IOException {

        BufferedImage image = ImageIO.read(new File("C:\\Users\\mk.jang\\Desktop\\TLC\\05_MI\\files\\image_1.png"));
        BufferedImage watermarkedImage = PDFUtil.addTextWatermark(image, "JANG MIN GYU - TWOLINECLOUD");
        byte[] imageBytes = PDFUtil.imageToByteArray(watermarkedImage, "png");

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes);
    }

    // 테스트 코드
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPdf(@RequestParam("file") MultipartFile pdfFile) {
        String uuidFileName = UUID.randomUUID().toString();
        pdfService.uploadPageImagesFromPDF(pdfFile, uuidFileName);
        Map<String, Object> response = new HashMap<>();
        response.put("uuid", uuidFileName);
        response.put("originalFilename", pdfFile.getOriginalFilename());
        response.put("message", "PDF 파일이 성공적으로 업로드되었습니다.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "데이타의 정형 데이타 파일 업로드", description = "데이타의 정형 데이타 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/goodsdatas/upload-file/structureddata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadRes> uploadStructuredDataFile(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Schema(implementation = TestReq.uploadStructuredDataFile.class) @RequestBody @Valid TestReq.uploadStructuredDataFile testReq) {

        return BaseResponseFactory.create(testService.uploadStructuredDataFile(file, testReq));
    }

    @Operation(summary = "정형 데이터 컴포넌트 저장", description = "정형 데이터 컴포넌트를 저장합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/goodsdatas/component")
    public BaseResponse<TestRes.SaveComponent> saveComponent(@RequestBody @Valid TestReq.saveComponent testReq) {
        return BaseResponseFactory.create(testService.saveComponent(testReq));
    }
}
