package com.sk.domain.shared.client.config;

import com.sk.common.http.RestClientBuilder;
import com.sk.domain.shared.client.AuthTokenApi;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.service.InMemoryTokenProvider;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RestClientHttpApiConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public InMemoryTokenProvider tokenProvider(
        AuthTokenApi authTokenApi,
        Clock clock,
        @Value("${api.auth-token.username}") String username,
        @Value("${api.auth-token.password}") String password,
        @Value("${api.auth-token.client-id}") String clientId) {
        // ✅ 만료 30초 전 선제 갱신
        return new InMemoryTokenProvider(
            authTokenApi, clock, Duration.ofSeconds(30),
            username, password, clientId
        );
    }

    /**
     * 'ModelAgent' Service API config
     *
     * @param url            url
     * @param connectTimeout connectTimeout
     * @param readTimeout    readTimeout
     * @return SampleRequestApi
     */
    @Bean
    public ModelAgentApi modelAgentApi(@Value("${api.model-agent.url}") String url,
                                       @Value("${api.model-agent.connection-timeout:1s}") Duration connectTimeout,
                                       @Value("${api.model-agent.read-timeout:10s}") Duration readTimeout,
                                       @Value("${api.model-agent.api-key}") String apiKey) {

        return new RestClientBuilder()
            .url(url)
            .headers("Authorization", "Bearer " + apiKey)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .build(ModelAgentApi.class);
    }

    @Bean
    public AuthTokenApi authTokenApi(
        @Value("${api.auth-token.url}") String url,
        @Value("${api.auth-token.connection-timeout:1s}") Duration connectTimeout,
        @Value("${api.auth-token.read-timeout:10s}") Duration readTimeout
        ) {

        return new RestClientBuilder()
            .url(url)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .build(AuthTokenApi.class);
    }

    @Bean
    public AxPlatformApi axPlatformApi(InMemoryTokenProvider tokenProvider,
                                       @Value("${api.ax-platform.url}") String url,
                                       @Value("${api.ax-platform.connection-timeout:1s}") Duration connectTimeout,
                                       @Value("${api.ax-platform.read-timeout:10s}") Duration readTimeout) {

        return new RestClientBuilder()
            .url(url)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .requestInterceptor(new AuthRetry401Interceptor(tokenProvider, 5))
            .build(AxPlatformApi.class);
    }
}
