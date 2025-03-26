package com.sk.domain.shared.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.shared.dto.response.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import io.jsonwebtoken.Claims;
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

	/**
	 * 토큰 갱신 (Refresh Token Rotation)
	 * 
	 * @param authHeader
	 * @param refreshToken
	 * @return
	 */
	public GenerateTokenRes generateToken(String authHeader, String refreshToken) {
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
				.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "사용자 정보가 없습니다."));

		// 토큰 생성
		String renewedAccessToken = jwtUtil.generateAccessToken(user);
		String renewedRefreshToken = jwtUtil.generateRefreshToken(user);

		return GenerateTokenRes.of(renewedAccessToken, renewedRefreshToken);
	}
}
