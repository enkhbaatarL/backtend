package com.sk.common.config.security.saml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.shared.entity.master.tlm.UserAccessLog;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tlm.UserAccessLogRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//TODO: 사용자 인증 성공 후 토큰 발급 로직 추가

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSamlUserAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final UserAccessLogRepository userAccessLogRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {

		Saml2Authentication saml2Authentication = (Saml2Authentication) authentication;
		Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) saml2Authentication.getPrincipal();

		log.info("SAML 사용자 인증 성공 {}", principal.getName());

		// SAML 속성에서 사용자 정보 추출
		String loginId = principal.getName();

		User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(loginId, UserRoleCd.USER)
				.orElseThrow(() -> new UsernameNotFoundException(String.format("SAML 사용자 인증 실패: %s", loginId)));

		// JWT 토큰 생성
		// String accessToken = jwtUtil.generateAccessToken(user);
		String refreshToken = jwtUtil.generateRefreshToken(user.getUserNm());

		// 토큰 응답 생성
		Map<String, Object> tokenResponse = new HashMap<>();
		// tokenResponse.put("access_token", accessToken);
		tokenResponse.put("refresh_token", refreshToken);
		tokenResponse.put("token_type", "Bearer");

		UserAccessLog userAccessLog = UserAccessLog.of(user.getUserLoginId());
		userAccessLogRepository.save(userAccessLog);

		// JSON 응답 전송
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpStatus.SC_OK);
		response.getWriter().write(new ObjectMapper().writeValueAsString(tokenResponse));
	}
}
