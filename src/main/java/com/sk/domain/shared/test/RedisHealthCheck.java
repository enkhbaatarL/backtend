package com.sk.domain.shared.test;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisHealthCheck {

    private final RedisConnectionFactory cf;

    public RedisHealthCheck(RedisConnectionFactory cf) {
        this.cf = cf;
    }

    @PostConstruct
    public void check() {
        try {
            String pong = cf.getConnection().ping();
            log.info("Redis Health Check 성공: ping 응답 = {}", pong);
        } catch (Exception e) {
            log.error("Redis Health Check 실패", e);
            throw e;
        }
    }

}
