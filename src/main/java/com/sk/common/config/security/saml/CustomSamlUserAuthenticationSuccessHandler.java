package com.sk.common.config.security.saml;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.entity.master.tlm.UserAccessLog;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tlm.UserAccessLogRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSamlUserAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	@Value("${spring.security.saml2.login-callback-url}")
	private String loginCallbackUrl;

	@Value("${spring.security.saml2.login-error-url}")
	private String loginErrorUrl;

	private final UserRepository userRepository;
	private final UserAccessLogRepository userAccessLogRepository;
	private final CustomerCompanyRepository customerCompanyRepository;
	private final StringRedisTemplate redisTemplate;

	private final int LOGIN_REDIS_KEY_EXPIRE_TIME = 60 * 10; // 로그인 레디스 키 유지시간 10분

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		log.info("SAML 사용자 인증 시작");

		Saml2Authentication saml2Authentication = (Saml2Authentication) authentication;
		Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) saml2Authentication.getPrincipal();

		log.info("SAML 사용자 인증 성공 {}", principal.getName());

		// SAML 속성에서 사용자 정보 추출
		String loginId = principal.getName();

		List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(loginId);
		if (users.isEmpty()) {
			log.error("SAML 사용자 인증 실패, 사용자 없음 : {}", loginId);
			response.sendRedirect(loginErrorUrl + "?errorCode=" + ResultCode.Error.AUTH_REQUIRED.getCode());
			throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, String.format("SAML 사용자 인증 실패, 사용자 없음: %s", loginId));
		}

		// users -> 모두 같은 회사이기 때문에 하나만 선택
		User user = users.get(0);
		// 사용자 정보 조회
		String currentDate = DateUtil.toYmd(LocalDate.now());
		CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(user.getCustomerCompany().getCustomerCompanyNo(), currentDate)
			.orElse(null);

		if (customerCompany == null) {
			log.error("SAML 사용자 인증 실패, 회사권한 없음 : {}", loginId);
			response.sendRedirect(loginErrorUrl + "?errorCode=" + ResultCode.Error.AUTH_REQUIRED.getCode());
			throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, String.format("SAML 사용자 인증 실패, 회사권한 없음: %s", loginId));
		}

		UserAccessLog userAccessLog = UserAccessLog.of(principal.getName());
		userAccessLogRepository.save(userAccessLog);

		String sessionId = UUID.randomUUID().toString();

		// GET /api/v1/saml2/callback 요청 시 사용자 정보를 가져오기 위한 데이터로 레디스에 저장
		redisTemplate.opsForValue().set(sessionId, principal.getName(), Duration.ofSeconds(LOGIN_REDIS_KEY_EXPIRE_TIME));

		// // 프론트엔드 콜백 URL로 리다이렉트, 액세스 토큰을 쿼리 파라미터로 전달
		response.sendRedirect(loginCallbackUrl + "?sessionId=" + sessionId);
	}
}
