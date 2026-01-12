package com.sk.domain.shared.client.service;

import com.sk.domain.shared.client.AuthTokenApi;
import com.sk.domain.shared.client.dto.response.AuthTokenRes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class InMemoryTokenProvider {
    private final AuthTokenApi authTokenApi;
    private final Clock clock;

    /** 만료 임박 선제 갱신 기준(예: 30초 전) */
    private final Duration refreshSkew;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile TokenHolder cached;

    private static final String GRANT_TYPE = "password";

    private final String username;
    private final String password;
    private final String clientId;

    public InMemoryTokenProvider(AuthTokenApi authTokenApi, Clock clock, Duration refreshSkew, String username, String password, String clientId) {
        this.authTokenApi = Objects.requireNonNull(authTokenApi);
        this.clock = Objects.requireNonNull(clock);
        this.refreshSkew = Objects.requireNonNull(refreshSkew);
        this.username = username;
        this.password = password;
        this.clientId = clientId;
    }

    /** 토큰이 있으면(만료 전이면) 반환. 단, "만료 임박"이면 empty 처리해서 호출자가 선제 갱신하도록 유도 */
    public Optional<String> getTokenIfUsable() {
        TokenHolder holder = this.cached;
        if (holder == null) return Optional.empty();

        Instant now = clock.instant();
        if (holder.expiresAt.isBefore(now)) {
            log.debug("Cached token expired. expiresAt={}, now={}", holder.expiresAt, now);
            return Optional.empty();
        }

        // 만료 임박이면 선제 갱신 대상으로 보고 "usable"로는 안 줌
        Instant refreshAt = holder.expiresAt.minus(refreshSkew);
        if (!refreshSkew.isZero() && !refreshSkew.isNegative() && !now.isBefore(refreshAt)) {
            log.info("Cached token is near expiry. now={}, refreshAt={}, expiresAt={}, token={}",
                now, refreshAt, holder.expiresAt, mask(holder.token));
            return Optional.empty();
        }

        return Optional.of(holder.token);
    }

    /** 요청 전에 호출: 토큰이 없거나 만료 임박이면 갱신하고 토큰 반환(실패 시 예외) */
    public String ensureFreshTokenOrThrow() {
        Optional<String> usable = getTokenIfUsable();
        if (usable.isPresent()) return usable.get();
        return refreshOrThrow("proactive");
    }

    /** 401 등으로 강제 갱신이 필요할 때 호출 */
    public String refreshOrThrow() {
        return refreshOrThrow("reactive");
    }

    private String refreshOrThrow(String reason) {
        refreshLock.lock();
        try {
            // lock 후 다시 확인: 다른 스레드가 갱신했을 수 있음
            Optional<String> usable = getTokenIfUsable();
            if (usable.isPresent()) {
                log.info("Token refresh skipped (already fresh by another thread). reason={}, token={}",
                    reason, mask(usable.get()));
                return usable.get();
            }

            log.info("Refreshing access token via Auth API... reason={}", reason);

            AuthTokenRes tr;
            try {
                tr = authTokenApi.auth(GRANT_TYPE, username, password, clientId);
            } catch (Exception e) {
                log.error("Auth API call failed while refreshing token. reason={}", reason, e);
                throw new TokenAcquireException("Auth API call failed", e);
            }

            if (tr == null || tr.accessToken() == null || tr.accessToken().isBlank()) {
                log.error("Auth API returned empty access_token. reason={}, response={}", reason, tr);
                throw new TokenAcquireException("Auth API returned empty access_token");
            }

            long expiresIn = Math.max(1, tr.expiresIn());
            Instant expiresAt = clock.instant().plusSeconds(expiresIn);

            this.cached = new TokenHolder(tr.accessToken(), expiresAt);

            Instant refreshAt = expiresAt.minus(refreshSkew);
            log.info("Token refreshed successfully. reason={}, token={}, refreshAt={}, expiresAt={}, expiresInSec={}",
                reason, mask(tr.accessToken()), refreshAt, expiresAt, expiresIn);

            return tr.accessToken();

        } finally {
            refreshLock.unlock();
        }
    }

    private record TokenHolder(String token, Instant expiresAt) {}

    public static final class TokenAcquireException extends RuntimeException {
        public TokenAcquireException(String message) { super(message); }
        public TokenAcquireException(String message, Throwable cause) { super(message, cause); }
    }

    private static String mask(String token) {
        if (token == null) return "null";
        int n = token.length();
        if (n <= 10) return "****(len=" + n + ")";
        return token.substring(0, 4) + "****" + token.substring(n - 4) + "(len=" + n + ")";
    }
}
