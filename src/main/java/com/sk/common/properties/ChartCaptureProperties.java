package com.sk.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "chart-capture")
public class ChartCaptureProperties {

    /** 프론트엔드 베이스 URL (예: https://front.example.com) */
    private String frontendBaseUrl = "http://localhost:5437";

    /** chart-capture JWT TTL (초 단위, 기본 5분) */
    private long jwtTtlSeconds = 300;

    /** Playwright /capture 타임아웃 (ms 단위, 기본 30초) */
    private int captureTimeoutMs = 30000;

}
