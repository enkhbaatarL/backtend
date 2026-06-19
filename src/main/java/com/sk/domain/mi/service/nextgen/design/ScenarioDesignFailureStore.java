package com.sk.domain.mi.service.nextgen.design;

import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignFailureCache;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 작업 실패 휘발 캐시 저장소 (Redis)
 * 실패 SSE 푸시는 best-effort라 구독 레이스(콜백이 SSE 연결보다 빠름)/연결 끊김으로 유실될 수 있음.
 * 마지막 실패를 임시 보관해 SSE 연결 시, PUSH
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioDesignFailureStore {

    private static final String KEY_PREFIX = "scenario-design:failure:";

    /** 실패 캐시 TTL — 설계 세션 도중 유지, 방치된 세션은 자연 만료 */
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate stringRedisTemplate;

    private static String key(Long scenarioGroupId) {
        return KEY_PREFIX + scenarioGroupId;
    }

    /** 실패를 TTL과 함께 저장한다(덮어쓰기). */
    public void save(Long scenarioGroupId, ScenarioDesignFailureCache cache) {
        stringRedisTemplate.opsForValue().set(key(scenarioGroupId), JsonUtil.toJson(cache), TTL);
        log.debug("[Design Failure] cached: groupId={}, type={}", scenarioGroupId, cache.type());
    }

    /** 저장된 실패를 조회한다. 없거나 파싱 실패 시 empty. */
    public Optional<ScenarioDesignFailureCache> find(Long scenarioGroupId) {
        String json = stringRedisTemplate.opsForValue().get(key(scenarioGroupId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonUtil.fromJson(json, ScenarioDesignFailureCache.class));
        } catch (Exception e) {
            log.warn("[Design Failure] cache parse failed: groupId={}, {}", scenarioGroupId, e.getMessage());
            return Optional.empty();
        }
    }

    /** 실패 캐시를 삭제한다 (후속 성공/초기화/확정 시). */
    public void delete(Long scenarioGroupId) {
        stringRedisTemplate.delete(key(scenarioGroupId));
        log.debug("[Design Failure] cache deleted: groupId={}", scenarioGroupId);
    }

}
