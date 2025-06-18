package com.sk.domain.shared.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SamlService {

    private final UserRepository userRepository;
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
    public GenerateTokenRes samlCallback(String sessionId, HttpServletResponse response) {
        // String userLoginId = redisTemplate.opsForValue().get(sessionId);
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

        // 사용자 정보 조회
        



        GenerateTokenRes generateTokenRes = authService.generateToken(user, response);

        redisTemplate.delete(sessionId);

        return generateTokenRes;
    }
}
