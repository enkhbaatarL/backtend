package com.sk.domain.test;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@Tag(name = "00. 테스트 샘플 API", description = "테스트 샘플 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(summary = "사용자 정보 조회 API", description = "사용자 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/user-info")
    public BaseResponse<TestRes.GetUserInfo> getUserInfo(@RequestBody @Valid TestReq.getUserInfo testReq) {
        return BaseResponseFactory.create(testService.getUserInfo(testReq));
    }

    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공", headers = {
            @Header(name = "Authorization", description = "새로운 Access Token (형식: Bearer <token>)", schema = @Schema(type = "string")),
            @Header(name = "Set-Cookie", description = "새로운 Refresh Token (쿠키: X-Refresh-Token=<token>; Max-Age=604800; Path=/; Secure; HttpOnly;)", schema = @Schema(type = "string"))
    }, content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class), examples = @ExampleObject(value = """
            {
                "status": true,
                "code": "string",
                "message": "string",
                "data": {
                    "accessToken": "string"
                }
            }
            """)))
    @Operation(summary = "테스트 유저 로그인", description = "테스트 유저 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/login/user")
    public BaseResponse<GenerateTokenRes> userLogin(@RequestBody @Valid TestReq.userLogin testLoginReq, HttpServletResponse response) {
        return BaseResponseFactory.create(testService.userLogin(testLoginReq, response));
    }

    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공", headers = {
            @Header(name = "Authorization", description = "새로운 Access Token (형식: Bearer <token>)", schema = @Schema(type = "string")),
            @Header(name = "Set-Cookie", description = "새로운 Refresh Token (쿠키: X-Refresh-Token=<token>; Max-Age=604800; Path=/; Secure; HttpOnly;)", schema = @Schema(type = "string"))
    }, content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class), examples = @ExampleObject(value = """
            {
                "status": true,
                "code": "string",
                "message": "string",
                "data": {
                    "accessToken": "string"
                }
            }
            """)))
    @Operation(summary = "테스트 어드민 로그인", description = "테스트 어드민 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/login/admin")
    public BaseResponse<GenerateTokenRes> adminLogin(@RequestBody @Valid TestReq.adminLogin testLoginReq, HttpServletResponse response) {
        return BaseResponseFactory.create(testService.adminLogin(testLoginReq, response));
    }

    @Operation(summary = "토큰 검증", description = "토큰 검증을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/verify-token")
    public BaseResponse<Boolean> verifyToken(@RequestBody @Valid TestReq.verifyToken testVerifyTokenReq) {
        return BaseResponseFactory.create(testService.checkToken(testVerifyTokenReq));
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

    @Operation(summary = "데이타의 정형 데이타 파일 업로드", description = "데이타의 정형 데이타 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/goodsdatas/upload-file/structureddata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadRes> uploadStructuredDataFile(
            @RequestPart(value = "file", required = true) MultipartFile file,
            @Schema(implementation = TestReq.uploadStructuredDataFile.class) @RequestPart(value = "data", required = true) @Valid TestReq.uploadStructuredDataFile testReq) {
        return BaseResponseFactory.create(testService.uploadStructuredDataFile(file, testReq));
    }

    @Operation(summary = "Excel upload API", description = "Excel 파일을 업로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> uploadExcelFile(@RequestParam("file") MultipartFile file) throws IOException {
        return BaseResponseFactory.create("success");
    }

    // [관리자 화면] 오직 관리자만 접근가능한 화면 = 관리자 화면은 95% 이상 이 권한으로 매칭될 것으로 생각함
    @RequiredPermission(roles = {Role.ADMIN})
    @GetMapping("/admin-management/goods/{goodsNo}/graph0")
    public String test1() {
        return "OK";
    }

    // [관리자 화면]
    // 관리자
    // 운영자(oauth 토큰 발급 / 관리회사 전용(private 데이터 업로드를 위함))
    // 서브 관리자(oauth 토큰 발급 / 운영회사 전용(public 데이터 업로드를 위함))
    // RPA? 거기서 사용되는 용도
    @RequiredPermission(roles = {Role.ADMIN, Role.OPERATOR, Role.API_SUB_ADMIN})
    @GetMapping("/admin-management/goods/{goodsNo}/graph1")
    public String test2() {
        return "OK";
    }

    // [사용자 화면] - 사용자 화면중에서 그냥 상품에 대한 권한없이 다 볼 수 있는 화면
    @RequiredPermission(roles = {Role.USER, Role.ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY // 타사권한 가진 어드민을 의미함
    )
    @GetMapping("/admin-management/goods/{goodsNo}/graph2")
    public String test3() {
        return "OK";
    }

    // [사용자 화면] - 사용자 화면중에서 그냥 상품에 대한 VIEW 권한이 필요한 화면
    @RequiredPermission(roles = {Role.USER, Role.ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY, // 타사권한 가진 어드민을 의미함
            requireViewPermission = true, goodsParam = "goodsNo" // 상품 단위로 권한을 체크하기 위함
    )
    @GetMapping("/admin-management/goods/{goodsNo}/graph3")
    public String test4() {
        return "OK";
    }

    // [사용자 화면] - 사용자 화면중에서 그냥 상품에 대한 DOWNLOAD 권한이 필요한 화면
    @RequiredPermission(roles = {Role.USER, Role.ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY, // 타사권한 가진 어드민을 의미함
            requireDownloadPermission = true, goodsParam = "goodsNo" // 상품 단위로 권한을 체크하기 위함
    )
    @GetMapping("/admin-management/goods/{goodsNo}/graph4")
    public String test5() {
        return "OK";
    }

    @GetMapping("/encodeToBase64")
    public String encodeToBase64(@RequestParam("plaintext") String plaintext) {
        return testService.encodeToBase64(plaintext.getBytes());
    }

    @GetMapping("/decodeFromBase64")
    public String decodeFromBase64(@RequestParam("ciphertext") String ciphertext) {
        return new String(testService.decodeFromBase64(ciphertext));
    }

    // 이메일 발송 테스트
    @PostMapping("/send-email")
    public Boolean sendEmail(@RequestBody @Valid TestReq.sendEmail testReq) {
        return testService.sendEmail(testReq);
    }

    // BCrypt 해시 생성 테스트
    @Operation(summary = "BCrypt 해시 생성 테스트", description = "평문 비밀번호를 BCrypt 해시로 변환합니다.")
    @ApiResponse(responseCode = "200", description = "BCrypt 해시 생성 성공")
    @PostMapping("/bcrypt/generate-hash")
    public BaseResponse<String> generateBcryptHash(@RequestParam("password") String password) {
        return BaseResponseFactory.create(testService.generateBcryptHash(password));
    }

    // BCrypt 해시 검증 테스트
    @Operation(summary = "BCrypt 해시 검증 테스트", description = "평문 비밀번호와 BCrypt 해시를 비교하여 검증합니다.")
    @ApiResponse(responseCode = "200", description = "BCrypt 해시 검증 성공")
    @PostMapping("/bcrypt/verify-password")
    public BaseResponse<Boolean> verifyBcryptPassword(
            @RequestParam("password") String password,
            @RequestParam("hashedPassword") String hashedPassword) {
        return BaseResponseFactory.create(testService.verifyBcryptPassword(password, hashedPassword));
    }

    // 비밀번호 강도 검증 테스트
    @Operation(summary = "비밀번호 강도 검증 테스트", description = "비밀번호 강도를 검증합니다.")
    @ApiResponse(responseCode = "200", description = "비밀번호 강도 검증 성공")
    @PostMapping("/bcrypt/validate-password-strength")
    public BaseResponse<Boolean> validatePasswordStrength(@RequestParam("password") String password) {
        return BaseResponseFactory.create(testService.validatePasswordStrength(password));
    }

    // // 레디스 캐시 삭제
    // @Operation(summary = "레디스 캐시 삭제", description = "레디스 캐시를 삭제합니다.")
    // @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    // @DeleteMapping("/redis-cache")
    // public Boolean deleteRedisCache(@RequestBody @Valid TestReq.deleteRedisCache testReq) {
    //     return testService.deleteRedisCache(testReq);
    // }

    // // 레디스 조회
    // @Parameters({
    //         @Parameter(name = "key", description = "조회하고자 하는 redis 키(type : key*), 키가 없으면 모든 키를 조회합니다.", required = false),
    //         @Parameter(name = "size", description = "크기 (기본값 : 1000)", required = false)
    // })
    // @Operation(summary = "레디스 조회", description = "레디스 조회를 진행합니다.")
    // @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    // @GetMapping("/redis-cache")
    // public BaseResponse<Map<String, Object>> getRedisCache(
    //         @RequestParam(value = "key", required = false) String key,
    //         @RequestParam(value = "size", required = false, defaultValue = "1000") @Min(1) int size) {
    //     return BaseResponseFactory.create(testService.getRedisCache(key, size));
    // }
}
