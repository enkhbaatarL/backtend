package com.sk.domain.mi.service.nextgen.design;

import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioValidationCache;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 검토 결과 휘발 캐시 저장소 (Redis). DB에 저장하지 않는다.
 * <p>
 * 검토 단계의 reconcile table + 정합성 결과를 임시 보관하며,
 * GET /design 오버레이(find), reset/확정 시 정리(delete)에 재사용된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioDesignValidationStore {

    private static final String KEY_PREFIX = "scenario-design:validation:";

    /** 검토 캐시 TTL — 설계 세션 도중 유지, 방치된 세션은 자연 만료 */
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate stringRedisTemplate;

    private static String key(Long scenarioGroupId) {
        return KEY_PREFIX + scenarioGroupId;
    }

    /** 검토 결과를 TTL과 함께 저장한다(덮어쓰기). */
    public void save(Long scenarioGroupId, ScenarioValidationCache cache) {
        stringRedisTemplate.opsForValue().set(key(scenarioGroupId), JsonUtil.toJson(cache), TTL);
        log.debug("[Design Validation] cached: groupId={}, isValid={}", scenarioGroupId, cache.isValid());
    }

    /** 저장된 검토 결과를 조회한다. 없거나 파싱 실패 시 empty(= DB 초기값으로 폴백). */
    public Optional<ScenarioValidationCache> find(Long scenarioGroupId) {
        String json = stringRedisTemplate.opsForValue().get(key(scenarioGroupId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonUtil.fromJson(json, ScenarioValidationCache.class));
        } catch (Exception e) {
            log.warn("[Design Validation] cache parse failed: groupId={}, {}", scenarioGroupId, e.getMessage());
            return Optional.empty();
        }
    }

    /** 검토 캐시를 삭제한다 (전체 초기화/확정 시). */
    public void delete(Long scenarioGroupId) {
        stringRedisTemplate.delete(key(scenarioGroupId));
        log.debug("[Design Validation] cache deleted: groupId={}", scenarioGroupId);
    }
}
