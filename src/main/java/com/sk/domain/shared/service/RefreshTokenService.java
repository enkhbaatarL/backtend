package com.sk.domain.shared.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final String PREFIX = "auth:";
    private final String NAME_SPACE = "refresh-token::";

    private final StringRedisTemplate redisTemplate;

    /**
     * 리프레시 토큰을 저장하고 Unix 타임스탬프 기반 만료 시간 설정
     *
     * @param userNo             사용자 번호
     * @param refreshToken       리프레시 토큰
     * @param expirationUnixTime 만료될 Unix 타임스탬프 (초 단위)
     */
    public void saveRefreshToken(Integer userNo, String refreshToken, long expirationUnixTime) {
        String key = generateRefreshTokenKey(userNo);

        // 현재 시간 (초 단위)
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long ttlSeconds = expirationUnixTime - currentTimeSeconds;

        // 토큰 저장 및 TTL 설정
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(Integer userNo) {
        String key = generateRefreshTokenKey(userNo);
        return redisTemplate.opsForValue().get(key);
    }

    // 리프레시 토큰 삭제
    public void deleteRefreshToken(Integer userNo) {
        String key = generateRefreshTokenKey(userNo);
        redisTemplate.delete(key);
    }

    // 토큰 검증 로직
    public boolean validateRefreshToken(Integer userNo, String refreshToken) {
        String storedToken = getRefreshToken(userNo);
        return ObjectUtils.isNotEmpty(storedToken) && storedToken.equals(refreshToken);
    }

    // Redis 키 생성
    private String generateRefreshTokenKey(Integer userNo) {
        return "%s%s%d".formatted(PREFIX, NAME_SPACE, userNo);
    }

}
