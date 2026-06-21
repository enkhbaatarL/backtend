package com.sk.domain.mi.service.nextgen.design;

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
 * 이벤트 단위 설계 점유 락 (Redis). 설계 시작(startDesign) 동시 호출 가드
 * 1. DB에 시나리오 그룹 row가 생기기 전을 설계가능 상태를 의도적으로 막는다.
 * 2. 그룹 생성 이후의 점유 지속/해제는 DB(scenarioDesignStatus, 그룹 삭제 시 designable)가 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDesignLockStore {

    private static final String KEY_PREFIX = "scenario-design:event-lock:";

    private static final Duration TTL = Duration.ofSeconds(60);

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

    private static String key(Long miEventId) {
        return KEY_PREFIX + miEventId;
    }

    /**
     * 이벤트 점유 락을 획득
     * @return 획득 성공 시 해제용 토큰, 이미 점유 중이면 empty
     */
    public Optional<String> tryAcquire(Long miEventId) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key(miEventId), token, TTL);
        if (Boolean.TRUE.equals(acquired)) {
            return Optional.of(token);
        }
        log.debug("[Design Lock] acquire skipped (already locked): eventId={}", miEventId);
        return Optional.empty();
    }

    /**
     * 락 해제
     * */
    public void release(Long miEventId, String token) {
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(key(miEventId)), token);
        } catch (Exception e) {
            log.warn("[Design Lock] release failed: eventId={}, {}", miEventId, e.getMessage());
        }
    }

}
