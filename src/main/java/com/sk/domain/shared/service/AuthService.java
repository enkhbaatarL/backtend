package com.sk.domain.shared.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JwtUtil;
import com.sk.common.properties.JwtProperties;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
	private final CustomerCompanyRepository customerCompanyRepository;
	private final StringRedisTemplate redisTemplate;
	private final RefreshTokenService refreshTokenService;
	private final JwtProperties jwtProperties;

	private final String ACCESS_TOKEN_HEADER = "Authorization";
	private final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

	/**
	 * 공통 로그인 콜백 (SAML, 외부 로그인 모두 사용)
	 *
	 * @param sessionId Redis에 저장된 세션 ID
	 * @param response HTTP 응답
	 * @return 토큰 생성 결과
	 */
	@Transactional
	public GenerateTokenRes loginCallback(String sessionId, HttpServletRequest request, HttpServletResponse response) {
		log.info("로그인 콜백 시작: sessionId={}", sessionId);

		// 1. Redis에서 사용자 로그인 ID 조회
		String userLoginId = redisTemplate.opsForValue().get(sessionId);
		if (userLoginId == null) {
			log.error("로그인 콜백 실패, 세션 만료 또는 존재하지 않음: {}", sessionId);
			throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED,
				String.format("로그인 콜백 실패, 세션 정보 없음: %s", sessionId));
		}

		// 2. 사용자 조회
		List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(userLoginId);
		if (users.isEmpty()) {
			log.error("로그인 콜백 실패, 사용자 없음: {}", userLoginId);
			throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED,
				String.format("로그인 콜백 실패, 사용자 없음: %s", userLoginId));
		}

		// 3. 사용자 선택 (ADMIN, OPERATOR 우선)
		User user = users.stream()
			.filter(u -> u.getUserRoleCd() != null &&
				(u.getUserRoleCd().equals(UserRoleCd.ADMIN) || u.getUserRoleCd().equals(UserRoleCd.OPERATOR)))
			.findFirst()
			.orElse(users.get(0));

		// 4. 회사 권한 검증
		String currentDate = DateUtil.toYmd(LocalDate.now());
		CustomerCompany customerCompany = customerCompanyRepository
			.findActiveByCustomerCompanyNo(user.getCustomerCompany().getCustomerCompanyNo(), currentDate)
			.orElse(null);

		if (customerCompany == null) {
			log.error("로그인 콜백 실패, 회사권한 없음: {}", userLoginId);
			throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED,
				String.format("로그인 콜백 실패, 회사권한 없음: %s", userLoginId));
		}

		// 5. 토큰 생성
		GenerateTokenRes tokenRes = generateToken(user, request, response);

		// 6. Redis 세션 정리
		redisTemplate.delete(sessionId);

		log.info("로그인 콜백 성공: {}", userLoginId);
		return tokenRes;
	}

	/**
	 * 토큰 갱신 (Refresh Token Rotation)
	 *
	 * @param authHeader
	 * @param refreshToken
	 * @return
	 */
	@Transactional
	public GenerateTokenRes generateToken(String authHeader, String refreshToken, HttpServletRequest request, HttpServletResponse response) {
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

		return generateToken(user, request, response);
	}

	/**
	 * 사용자 기반 토큰 생성 (내부 공통 메서드)
	 *
	 * @param user 사용자 정보
	 * @param request HTTP 요청
	 * @param response HTTP 응답
	 * @return 토큰 생성 결과
	 */
	public GenerateTokenRes generateToken(User user, HttpServletRequest request, HttpServletResponse response) {
		String renewedAccessToken = jwtUtil.generateAccessToken(user, request.getServerName());
		String renewedRefreshToken = jwtUtil.generateRefreshToken(user);

		// 리프레시 토큰을 쿠키에 설정
		ResponseCookie responseRefreshCookie = ResponseCookie.from(REFRESH_TOKEN_HEADER, renewedRefreshToken)
				.maxAge(jwtProperties.resolveSaml2RefreshTokenExpiry(user.getUserLoginId()).getSeconds() * 10) // 의도적으로 토큰 만료기간보다 10배 더 길게 설정(토큰 갱신 시에 파라미터 에러가 아닌 Auth 에러를 발생시키기 위함)
				.path("/")
				.secure(true)
				.httpOnly(true)
                .sameSite("None")
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, responseRefreshCookie.toString());

		return GenerateTokenRes.of(renewedAccessToken);
	}
}
