package com.sk.domain.mi.service.nextgen.priceinterp;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 그룹 단위 가격전망 해석 생성 락 (Redis NX). generate 동시 호출 가드
 * 커밋 이후의 중복 차단은 DB status(GENERATING 스킵)가 인계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceInterpretationGenerationLockStore {

    private static final String KEY_PREFIX = "scenario-price-interp:generation-lock:";

    private static final Duration TTL = Duration.ofSeconds(90);

    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of(
        """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """,
        Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    private static String key(Long scenarioGroupId) {
        return KEY_PREFIX + scenarioGroupId;
    }

    /**
     * 그룹 생성 락을 획득
     * @return 획득 성공 시 해제용 토큰, 이미 다른 생성 요청이 처리 중이면 empty
     */
    public Optional<String> tryAcquire(Long scenarioGroupId) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key(scenarioGroupId), token, TTL);
        if (Boolean.TRUE.equals(acquired)) {
            return Optional.of(token);
        }
        log.debug("[PriceInterp Lock] acquire skipped (already locked): groupId={}", scenarioGroupId);
        return Optional.empty();
    }

    /**
     * 락 해제
     */
    public void release(Long scenarioGroupId, String token) {
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(key(scenarioGroupId)), token);
        } catch (Exception e) {
            log.warn("[PriceInterp Lock] release failed: groupId={}, {}", scenarioGroupId, e.getMessage());
        }
    }

}
