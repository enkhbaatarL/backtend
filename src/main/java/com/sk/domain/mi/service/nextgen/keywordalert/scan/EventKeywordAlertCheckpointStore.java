package com.sk.domain.mi.service.nextgen.keywordalert.scan;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 키워드 알림 스캔 체크포인트 (Redis)
 * - 키: event-keyword-alert:scan-checkpoint (TTL 없음)
 * - 값: 마지막 처리 이벤트의 mod_dtm (DB 값만 사용 → 서버 시계 차이 영향 없음)
 * - 없을 때: 스캔 서비스가 현재 최대 mod_dtm으로 초기화
 */
@Slf4j
@Component
public class EventKeywordAlertCheckpointStore {

    private static final String CHECKPOINT_KEY = EventKeywordAlertScanLockStore.KEY_PREFIX + ":scan-checkpoint";

    private final StringRedisTemplate stringRedisTemplate;

    public EventKeywordAlertCheckpointStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Optional<LocalDateTime> get() {
        String value = stringRedisTemplate.opsForValue().get(CHECKPOINT_KEY);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(value));
        } catch (RuntimeException e) {
            // 형식이 깨진 값은 없는 것으로 보고 재초기화함 (기존 이벤트 무시 정책과 동일)
            log.warn("[Keyword Alert Scan] invalid checkpoint value ignored: {}", value);
            return Optional.empty();
        }
    }

    public void save(LocalDateTime checkpoint) {
        stringRedisTemplate.opsForValue().set(CHECKPOINT_KEY, checkpoint.toString());
    }

}
