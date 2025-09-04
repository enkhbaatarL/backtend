package com.sk.domain.shared.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.dto.request.external.ExternalLoginReq;
import com.sk.domain.shared.dto.response.external.ExternalLoginRes;
import com.sk.domain.shared.entity.master.tlm.UserAccessLog;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tlm.UserAccessLogRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalService {

    private final UserRepository userRepository;
    private final UserAccessLogRepository userAccessLogRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final StringRedisTemplate redisTemplate;
    private final int LOGIN_REDIS_KEY_EXPIRE_TIME = 60 * 10; // 로그인 레디스 키 유지시간 10분
    private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder(12);

    @Transactional
    public ExternalLoginRes externalLogin(ExternalLoginReq loginReq, HttpServletResponse response) {
        log.info("외부 로그인 시작: {}", loginReq.loginId());

        // 1. 사용자 조회
        List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(loginReq.loginId());
        if (users.isEmpty()) {
            log.error("외부 로그인 실패, 사용자 없음: {}", loginReq.loginId());
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED,
                String.format("외부 로그인 실패, 사용자 없음: %s", loginReq.loginId()));
        }

        // 2. 패스워드 검증
        User user = users.get(0);
        if (user.getPassword() == null || !bcryptPasswordEncoder.matches(loginReq.password(), user.getPassword())) {
            log.error("외부 로그인 실패, 패스워드 불일치: {}", loginReq.loginId());
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED,
                String.format("외부 로그인 실패, 패스워드 불일치: %s", loginReq.loginId()));
        }

        // 3. 회사 권한 검증
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository
            .findActiveByCustomerCompanyNo(user.getCustomerCompany().getCustomerCompanyNo(), currentDate)
            .orElse(null);

        if (customerCompany == null) {
            log.error("외부 로그인 실패, 회사권한 없음: {}", loginReq.loginId());
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED,
                String.format("외부 로그인 실패, 회사권한 없음: %s", loginReq.loginId()));
        }

        // 4. 접근 로그 저장
        UserAccessLog userAccessLog = UserAccessLog.of(loginReq.loginId());
        userAccessLogRepository.save(userAccessLog);

        // 5. 세션 ID 생성 및 Redis 저장
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(sessionId, loginReq.loginId(), Duration.ofSeconds(LOGIN_REDIS_KEY_EXPIRE_TIME));

        log.info("외부 로그인 성공: {}", loginReq.loginId());
        return ExternalLoginRes.of(sessionId);
    }
}