package com.sk.domain.admin.service;

import com.sk.common.config.redis.CacheType;
import com.sk.common.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenQuotaGuard {

    @Value("${features.validation.model-agent.token-usage-limit:100000000}")
    private long tokenUsageLimit;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> quotaScript;

    public boolean hasCapacity(Integer companyId) {
        String key = key(companyId);
        String v = redis.opsForValue().get(key);
        long used = (v == null) ? 0 : Long.parseLong(v);
        return used < tokenUsageLimit;
    }

    // ② 실제 사용량 누적 & 초과 여부 반환 (모델 호출 후)
    public QuotaResult addAndCheck(Integer companyId, long delta) {
        String key = key(companyId);
        long usage = redis.execute(quotaScript, Collections.singletonList(key), String.valueOf(delta), String.valueOf(tokenUsageLimit));
        // 1 = 한도 내, 0 = 한도 초과
        return new QuotaResult(tokenUsageLimit >= usage, usage);
    }

    // ───────── private ─────────
    private String key(Integer companyId) {
        String ym = YearMonth.now(ZoneId.of("Asia/Seoul")).format(DateUtil.YYYYMM);
        return String.format(CacheType.CHATBOT_TOKEN_USAGE.getCachePrefix() + CacheType.CHATBOT_TOKEN_USAGE.getCacheName(), companyId, ym);
    }

    public record QuotaResult(
        boolean allowed,
        long usage
    ) {}

}
