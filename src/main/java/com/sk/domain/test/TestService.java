package com.sk.domain.test;

import com.sk.common.utils.BcryptHashUtil;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.service.ExcelService;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tcm.ComponentInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.AuthService;
import com.sk.domain.shared.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final ExcelService excelService;
    private final AuthService authService;
    private final MailService mailService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final BcryptHashUtil bcryptHashUtil;

    public TestRes.GetUserInfo getUserInfo(TestReq.getUserInfo testReq) {
        return TestRes.GetUserInfo.of(testReq);
    }

    /**
     * 사용자 로그인
     *
     * @param testLoginReq
     * @return
     */
    @Transactional
    public GenerateTokenRes userLogin(TestReq.userLogin testLoginReq, HttpServletResponse response) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                UserRoleCd.USER)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "User information could not be found. userLoginId: " + testLoginReq.loginUserId()));

        return authService.generateToken(user, response);
    }

    /**
     * 관리자 로그인
     *
     * @param testLoginReq
     * @return
     */
    @Transactional
    public GenerateTokenRes adminLogin(TestReq.adminLogin testLoginReq, HttpServletResponse response) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String clientIp = request.getRemoteAddr();

        log.info("clientIp: {}", clientIp);

        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                testLoginReq.userRoleCd())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format(
                        "User information could not be found. userLoginId: %s, userRoleCd: %s", testLoginReq.loginUserId(),
                        testLoginReq.userRoleCd())));

        adminPermissionRepository.findByIdUserNoAndIdIpAddressAndIsDeletedFalse(user.getUserNo(), clientIp)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("The IP address is not allowed: %s", clientIp)));

        return authService.generateToken(user, response);
    }

    /**
     * 토큰 검증
     *
     * @param token
     * @return
     */
    public Boolean checkToken(TestReq.verifyToken testVerifyTokenReq) {
        return jwtUtil.isTokenValid(testVerifyTokenReq.token());
    }

    @Transactional
    public String getToken(String loginUserId, UserRoleCd role) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(loginUserId, role)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        return jwtUtil.generateAccessToken(user);
    }

    public String delete(Integer id) {
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(id)
                .orElseThrow();
        customerCompany.softDelete();
        return "ok";
    }

    // public List<Map<String, Object>> getDynamicData(TestReq.uploadStructuredDataFile testReq) {
    // ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(testReq.componentTypeId())
    // .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
    // String.format("Component information could not be found. componentTypeId: %s", testReq.componentTypeId())));

    // return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(componentInfo));
    // }

    /**
     * 정형 데이터 파일 검수 후 업로드
     *
     * @param file
     * @param testReq
     * @return
     */
    public FileUploadRes uploadStructuredDataFile(MultipartFile file, TestReq.uploadStructuredDataFile testReq) {
        try {
            // 컴포넌트 타입 조회
            ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(testReq.componentTypeId())
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                            String.format("Component information could not be found. componentTypeId: %s", testReq.componentTypeId())));

            // 엑셀 파일 검수 후 업로드
            boolean isUploaded = excelService.uploadStructuredData(file, componentInfo, testReq.criteriaDt(), null);

            return null;
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    public FileUploadRes uploadStructuredDataFileTest(MultipartFile file) {
        return null;
    }

    public String encodeToBase64(byte[] bytesToEncode) {
        return Base64.getEncoder().encodeToString(bytesToEncode);
    }

    public byte[] decodeFromBase64(String stringToDecode) {
        return Base64.getDecoder().decode(stringToDecode);
    }

    public Boolean sendEmail(TestReq.sendEmail testReq) {
        String to = testReq.email();
        String subject = "TEST MAIL";
        String content = """
                <h1>TEST MAIL</h1>
                <p>TEST MAIL</p>
                """;;
        mailService.sendMailNoAccessLog(to, subject, content);
        return true;
    }


    /**
     * BCrypt 해시 생성
     *
     * @param password 평문 비밀번호
     * @return BCrypt 해시된 비밀번호
     */
    public String generateBcryptHash(String password) {
        try {
            return bcryptHashUtil.generateBcryptHash(password);
        } catch (Exception e) {
            log.error("BCrypt 해시 생성 실패: {}", e.getMessage());
            throw AppErrorException.of(ResultCode.Error.SYS_UNKNOWN, "BCrypt 해시 생성에 실패했습니다.");
        }
    }

    /**
     * BCrypt 해시 검증
     *
     * @param password 평문 비밀번호
     * @param hashedPassword BCrypt 해시된 비밀번호
     * @return 검증 결과
     */
    public Boolean verifyBcryptPassword(String password, String hashedPassword) {
        try {
            return bcryptHashUtil.verifyBcryptPassword(password, hashedPassword);
        } catch (Exception e) {
            log.error("BCrypt 해시 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 비밀번호 강도 검증
     *
     * @param password 검증할 비밀번호
     * @return 검증 결과
     */
    public Boolean validatePasswordStrength(String password) {
        try {
            return bcryptHashUtil.validatePasswordStrength(password);
        } catch (Exception e) {
            log.error("비밀번호 강도 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    // public Boolean deleteRedisCache(TestReq.deleteRedisCache testReq) {
    //     // redisTemplate.delete(testReq.key());
    //     return true;
    // }

    // public Map<String, Object> getRedisCache(String key, int size) {
    //     List<String> keys = new ArrayList<>();

    //     // SCAN 옵션 설정
    //     ScanOptions scanOptions = ScanOptions.scanOptions()
    //             .match(key)
    //             .count(size)
    //             .build();

    //     // Redis 연결을 통해 SCAN 실행
    //     try (var cursor = redisTemplate.getConnectionFactory().getConnection().scan(scanOptions)) {
    //         while (cursor.hasNext()) {
    //             keys.add(new String(cursor.next()));
    //         }
    //     }

    //     // 키에 해당하는 값 조회
    //     Map<String, Object> result = keys.stream()
    //             .collect(Collectors.toMap(
    //                     k -> k,
    //                     k -> redisTemplate.opsForValue().get(k)
    //             ));

    //     return result;
    // }
}