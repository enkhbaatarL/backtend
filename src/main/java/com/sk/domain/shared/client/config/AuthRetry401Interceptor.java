package com.sk.domain.shared.client.config;

import com.sk.domain.shared.client.service.InMemoryTokenProvider;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

@Slf4j
public final class AuthRetry401Interceptor implements ClientHttpRequestInterceptor {
    private final InMemoryTokenProvider tokenProvider;
    private final int max401Retries;

    public AuthRetry401Interceptor(InMemoryTokenProvider tokenProvider, int max401Retries) {
        this.tokenProvider = tokenProvider;
        this.max401Retries = max401Retries;
    }

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request,
        byte[] body,
        ClientHttpRequestExecution execution
    ) throws IOException {

        String rid = UUID.randomUUID().toString().substring(0, 8);
        int unauthorizedCount = 0;

        URI uri = request.getURI();
        HttpMethod method = request.getMethod();

        log.debug("[{}] -> {} {}", rid, method, uri);

        // ✅ 요청 전에 선제 갱신(토큰 없거나 만료 임박이면 갱신)
        String token = tokenProvider.ensureFreshTokenOrThrow();
        log.debug("[{}] Proactive token ensured for {} {}.", rid, method, uri);

        while (true) {
            ClientHttpResponse response = null;
            try {
                response = execution.execute(withBearer(request, token), body);

                if (response.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                    log.debug("[{}] <- {} {} : {}", rid, method, uri, response.getStatusCode().value());
                    return response;
                }

                unauthorizedCount++;
                log.warn("[{}] <- {} {} : 401 Unauthorized (count={}/{})",
                    rid, method, uri, unauthorizedCount, max401Retries);

                response.close();

                if (unauthorizedCount > max401Retries) {
                    log.error("[{}] Too many 401 responses for {} {}. Failing.",
                        rid, method, uri);
                    throw new TooManyUnauthorizedException(
                        "401 returned more than " + max401Retries + " times for " + method + " " + uri
                    );
                }

                // ✅ 401이면 리액티브 갱신 후 재시도
                log.info("[{}] Refreshing token due to 401 for {} {} (attempt {}/{})",
                    rid, method, uri, unauthorizedCount, max401Retries);

                token = tokenProvider.refreshOrThrow();

                log.info("[{}] Retrying request after token refresh: {} {} (retry={}/{})",
                    rid, method, uri, unauthorizedCount, max401Retries);

            } catch (InMemoryTokenProvider.TokenAcquireException e) {
                log.error("[{}] Token acquire failed. Failing request: {} {}", rid, method, uri, e);
                throw e;
            } catch (IOException e) {
                log.error("[{}] IO error while calling {} {}", rid, method, uri, e);
                throw e;
            }
        }
    }

    private HttpRequest withBearer(HttpRequest original, String token) {
        return new HttpRequestWrapper(original) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.setBearerAuth(token);
                return headers;
            }
        };
    }

    public static final class TooManyUnauthorizedException extends RuntimeException {
        public TooManyUnauthorizedException(String message) { super(message); }
    }
}
