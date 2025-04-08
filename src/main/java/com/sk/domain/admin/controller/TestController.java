package com.sk.domain.admin.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.PDFUtil;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestLoginRes;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.admin.service.TestService;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.enums.UserRoleCd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "00. 테스트 샘플 API", description = "테스트 샘플 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    @Operation(summary = "사용자 정보 조회 API", description = "사용자 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/user-info")
    public BaseResponse<TestRes> getUserInfo(@RequestBody @Valid TestReq.getUserInfo testReq) {
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
    public BaseResponse<TestLoginRes> adminLogin(@RequestBody @Valid TestReq.userLogin testLoginReq) {
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

    @GetMapping(value = "/watermark", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getWatermarkedImage() throws IOException {

        BufferedImage image = ImageIO.read(new File("C:\\Users\\mk.jang\\Desktop\\TLC\\05_MI\\files\\image_1.png"));
        BufferedImage watermarkedImage = PDFUtil.addTextWatermark(image, "JANG MIN GYU - TWOLINECLOUD");
        byte[] imageBytes = PDFUtil.imageToByteArray(watermarkedImage, "png");

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes);
    }

}
