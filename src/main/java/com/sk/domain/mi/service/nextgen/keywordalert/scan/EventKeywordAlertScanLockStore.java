package com.sk.domain.mi.service.nextgen.keywordalert.scan;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 키워드 알림 스캔 락 (Redis) — 멀티노드 중 1개 노드만 스캔
 * - 획득: SET NX + 노드별 토큰, TTL 5분 (노드 중단 시 TTL 후 다른 노드가 이어받음)
 * - 해제: 토큰이 같을 때만 삭제 (Lua)
 */
@Slf4j
@Component
public class EventKeywordAlertScanLockStore {

    /** 키워드 알림 스캔 Redis 키 접두어 (락·체크포인트 공용) */
    static final String KEY_PREFIX = "event-keyword-alert";

    private static final String LOCK_KEY = KEY_PREFIX + ":scan-lock";

    private static final Duration TTL = Duration.ofMinutes(5);

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

    public EventKeywordAlertScanLockStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 스캔 락 획득
     * @return 획득 성공 시 해제용 토큰, 다른 노드가 스캔 중이면 empty
     */
    public Optional<String> tryAcquire() {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, TTL);
        if (Boolean.TRUE.equals(acquired)) {
            return Optional.of(token);
        }
        log.debug("[Keyword Alert Scan] lock skipped (another node is scanning)");
        return Optional.empty();
    }

    /** 스캔 락 해제 (토큰이 다르면 지우지 않음) */
    public void release(String token) {
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(LOCK_KEY), token);
        } catch (Exception e) {
            log.warn("[Keyword Alert Scan] lock release failed: {}", e.getMessage());
        }
    }

}
