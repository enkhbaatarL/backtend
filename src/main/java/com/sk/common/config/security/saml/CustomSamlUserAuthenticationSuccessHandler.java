package com.sk.common.config.security.saml;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sk.domain.shared.entity.master.tlm.UserAccessLog;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tlm.UserAccessLogRepository;
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

	private final UserRepository userRepository;
	private final UserAccessLogRepository userAccessLogRepository;
	private final StringRedisTemplate redisTemplate;

	private final int LOGIN_SESSION_EXPIRE_TIME = 60 * 10; //로그인 세션 유지시간 10분

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {

		Saml2Authentication saml2Authentication = (Saml2Authentication) authentication;
		Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) saml2Authentication.getPrincipal();

		log.info("SAML 사용자 인증 성공 {}", principal.getName());

		// SAML 속성에서 사용자 정보 추출
		String loginId = principal.getName();

		List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(loginId);
		if (users.isEmpty()) {
			String errorUrl = "http://localhost:3000/error?error=SAML_USER_NOT_FOUND";
			response.sendRedirect(errorUrl);
			throw new UsernameNotFoundException(String.format("SAML 사용자 인증 실패: %s", loginId));
		}

		UserAccessLog userAccessLog = UserAccessLog.of(principal.getName());
		userAccessLogRepository.save(userAccessLog);

		String sessionId = UUID.randomUUID().toString();

		redisTemplate.opsForValue().set(sessionId, principal.getName(), Duration.ofSeconds(LOGIN_SESSION_EXPIRE_TIME));

		// // 프론트엔드 콜백 URL로 리다이렉트, 액세스 토큰을 쿼리 파라미터로 전달
		String callbackUrl = "http://localhost:3000/callback?sessionId=" + sessionId;
		response.sendRedirect(callbackUrl);
	}
}
