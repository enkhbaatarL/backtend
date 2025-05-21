package com.sk.domain.shared.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.common.utils.JwtUtil;
import com.sk.common.properties.JwtProperties;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final RefreshTokenService refreshTokenService;
	private final JwtProperties jwtProperties;

	private final String ACCESS_TOKEN_HEADER = "Authorization";
	private final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
	// private final String ACCESS_TOKEN_RULE = "Bearer ";

	/**
	 * 토큰 갱신 (Refresh Token Rotation)
	 *
	 * @param authHeader
	 * @param refreshToken
	 * @return
	 */
	@Transactional
	public Boolean generateToken(String authHeader, String refreshToken, HttpServletResponse response) {
		String accessToken = authHeader.replace("Bearer ", "");

		// accessToken 검증(만료 상관없이)
		Claims accessTokenClaims = jwtUtil.extractClaimsIgnoreExpiration(accessToken);

		// refreshToken 검증
		if (jwtUtil.isTokenValid(refreshToken)) {
			if (!refreshTokenService.validateRefreshToken(accessTokenClaims.get("userNo", Integer.class), refreshToken)) {
				throw CustomAuthenticationException.of(AuthComponent.TOKEN_VALIDATION, ResultCode.Error.AUTH_TOKEN_INVALID);
			}
		}

		User user = userRepository.findById(accessTokenClaims.get("userNo", Integer.class))
				.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
						String.format("User information could not be found. userNo: %s", accessTokenClaims.get("userNo", Integer.class))));

		generateToken(user, response);

		return Boolean.TRUE;
	}

	public GenerateTokenRes generateToken(User user, HttpServletResponse response) {
		String renewedAccessToken = jwtUtil.generateAccessToken(user);
		String renewedRefreshToken = jwtUtil.generateRefreshToken(user);

		// 엑세스 토큰을 쿠키에 설정
		ResponseCookie responseAccessTokenCookie = ResponseCookie.from(ACCESS_TOKEN_HEADER, renewedAccessToken)
				.maxAge(jwtProperties.getAccessTokenExpiry() / 1000)
				.path("/")
				.secure(true)
				.httpOnly(true)
				.build();


		// 리프레시 토큰을 쿠키에 설정
		ResponseCookie responseRefreshCookie = ResponseCookie.from(REFRESH_TOKEN_HEADER, renewedRefreshToken)
				.maxAge(jwtProperties.getRefreshTokenExpiry() / 1000)
				.path("/")
				.secure(true)
				.httpOnly(true)
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, responseRefreshCookie.toString());

		// ResponseCookie responseAccessTokenCookie = ResponseCookie.from(ACCESS_TOKEN_HEADER, ACCESS_TOKEN_RULE + renewedAccessToken)
		// 		.maxAge(jwtProperties.getAccessTokenExpiry() / 1000)
		// 		.path("/")
		// 		.secure(true)
		// 		.httpOnly(true)
		// 		.build();

		// response.addHeader(HttpHeaders.SET_COOKIE, responseAccessTokenCookie.toString());

		// response.addHeader(ACCESS_TOKEN_HEADER, ACCESS_TOKEN_RULE + renewedAccessToken);

		response.addHeader(HttpHeaders.SET_COOKIE, responseAccessTokenCookie.toString());

		return GenerateTokenRes.of(renewedAccessToken);
	}
}
