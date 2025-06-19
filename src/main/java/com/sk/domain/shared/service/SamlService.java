package com.sk.domain.shared.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tam.AdminPermission;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SamlService {

    private final UserRepository userRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AuthService authService;

    /**
     * SAML 콜백 API
     * 
     * @param sessionId
     * @param response
     * @return
     */
    @Transactional
    public GenerateTokenRes samlCallback(String sessionId, HttpServletResponse response, HttpServletRequest request) {
        String userLoginId = redisTemplate.opsForValue().get(sessionId);

        if (userLoginId == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("SAML 사용자 인증 실패: %s", sessionId));
        }

        List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(userLoginId);
        if (users.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("SAML 사용자 인증 실패: %s", userLoginId));
        }

        User user = users.stream()
                .filter(u -> u.getUserRoleCd() != null &&
                        (u.getUserRoleCd().equals(UserRoleCd.ADMIN) || u.getUserRoleCd().equals(UserRoleCd.OPERATOR)))
                .findFirst()
                .orElse(users.get(0));





        // => IP는 따로 API 빼기

        // 관리자 또는 운영자의 경우 허용된 IP에서만 접근 가능
        if (user.getUserRoleCd().equals(UserRoleCd.ADMIN) || user.getUserRoleCd().equals(UserRoleCd.OPERATOR)) {
            // 클라이언트 IP 주소 추출
            String clientIp = getClientIp(request);

            List<AdminPermission> existingPermissions = adminPermissionRepository.findByIdUserNoAndIsDeletedFalse(user.getUserNo());

            // IP 검증 로직 (예: 허용된 IP 목록에 clientIp가 포함되어 있는지 확인)
            boolean isAllowedIp = existingPermissions.stream()
                .anyMatch(permission -> permission.getId().getIpAddress().equals(clientIp)); // 허용 IP 필드가 있다고 가정

            if (!isAllowedIp) {
                throw AppErrorException.of(ResultCode.Error.AUTH_IP_NOT_ALLOWED, String.format("SAML 사용자 인증 실패: %s", userLoginId));
            }
        }



        

        // 사용자 정보 조회
        String currentDate = DateUtil.toYmd(LocalDate.now());
        customerCompanyRepository.findActiveByCustomerCompanyNo(user.getCustomerCompany().getCustomerCompanyNo(), currentDate)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("SAML 사용자 인증 실패: %s", userLoginId)));


        GenerateTokenRes generateTokenRes = authService.generateToken(user, response);

        redisTemplate.delete(sessionId);

        return generateTokenRes;
    }

    /**
 * 클라이언트 IP 주소를 추출하는 메서드
 * 
 * @param request
 * @return 클라이언트 IP 주소
 */
private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
    }
    // X-Forwarded-For 헤더에 여러 IP가 포함된 경우 첫 번째 IP만 사용
    if (ip != null && ip.contains(",")) {
        ip = ip.split(",")[0].trim();
    }
    return ip;
}
}
