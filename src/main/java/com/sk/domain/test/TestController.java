package com.sk.domain.test;

import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.MergedModelInputFiles;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelInputFile;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelParamAdjustment;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelParamExcelMergeService;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.UserRoleCd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Profile("!dedicated")
@Tag(name = "00. 테스트 샘플 API", description = "테스트 샘플 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final ModelParamExcelMergeService modelParamExcelMergeService;

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
    public BaseResponse<GenerateTokenRes> userLogin(@RequestBody @Valid TestReq.userLogin testLoginReq, HttpServletRequest request, HttpServletResponse response) {
        return BaseResponseFactory.create(testService.userLogin(testLoginReq, request, response));
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
            @RequestParam("role") UserRoleCd role, HttpServletRequest request) {
        return BaseResponseFactory.create(testService.getToken(loginUserId, role, request));
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

    @Operation(summary = "상품 데이터 파일 연결 검증", description = "tmd_gdsdatam, tcm_filel, tmd_gdsdatactgrl 간의 연관 관계 불일치를 검사합니다.")
    @GetMapping("/validate-goods-data-files")
    public BaseResponse<String> validateGoodsDataFiles(
            @RequestParam(value = "file", defaultValue = "false") boolean file,
            @RequestParam(value = "count", defaultValue = "all") String count,
            @RequestParam(value = "gdsNo", required = false) Integer gdsNo) {
        testService.validateGoodsDataFiles(file, count, gdsNo);
        return BaseResponseFactory.create("Validation logic executed. Check logs for results.");
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
        // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102279', 오탐 -> plaintext는 필수 RequestParam으로 Spring이 not null 보장(누락 시 400).
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

    @Operation(
            summary = "확정 파라미터 병합 결과 다운로드 테스트",
            description = """
                    확정 파라미터(confirmation_parameters) JSON array를 그대로 body로 보내면 병합 컴포넌트를 태워 결과 입력 파일 4개(Demand/Supply/Trade 병합본 + Output 원본)를 zip으로 내려받습니다. <br>
                    body는 tmm_mi_model_param_proposal_m.confirmation_parameters 컬럼 값을 그대로 붙여넣으면 됩니다. <br>
                    응답: `merged-model-input-{오늘날짜}.zip` (병합 대상 없는 파일은 원본 그대로 포함). <br>
                    좌표 결함이 있으면 400 (`수급모델 입력 병합 실패 [파라미터 | 파일>시트] — 사유`).
                    """
    )
    @ApiResponse(responseCode = "200", description = "병합 및 다운로드 성공")
    @PostMapping(value = "/model-inputs/merge/download", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/zip")
    public ResponseEntity<InputStreamResource> downloadMerged(@RequestBody String confirmationParametersJson) {
        MergedModelInputFiles merged = modelParamExcelMergeService.merge(ModelParamAdjustment.listFromJson(confirmationParametersJson));

        byte[] data = zipModelInputs(merged);
        String filename = "merged-model-input-%s.zip".formatted(DateUtil.currentYmd());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);

        return new ResponseEntity<>(new InputStreamResource(new ByteArrayInputStream(data)), headers, HttpStatus.OK);
    }

    private byte[] zipModelInputs(MergedModelInputFiles merged) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ModelInputFile file : ModelInputFile.values()) {
                zip.putNextEntry(new ZipEntry(file.getBasecaseFileName()));
                zip.write(merged.content(file));
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "병합 결과 zip 생성에 실패했습니다.");
        }
    }

}
