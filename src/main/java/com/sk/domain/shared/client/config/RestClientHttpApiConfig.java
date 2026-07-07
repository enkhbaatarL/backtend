package com.sk.domain.shared.client.config;

import com.sk.common.http.RestClientBuilder;
import com.sk.common.http.RestClientDefaultConfig;
import com.sk.domain.shared.client.AuthTokenApi;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.PriceInterpretationAgentApi;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.client.service.InMemoryTokenProvider;
import com.sk.domain.shared.config.FileCryptoApiProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
    public ScenarioDesignAgentApi scenarioDesignAgentApi(@Value("${api.scenario-design-agent.url:http://40.82.137.178:8001}") String url,
                                                         @Value("${api.scenario-design-agent.connection-timeout:1s}") Duration connectTimeout,
                                                         @Value("${api.scenario-design-agent.read-timeout:60s}") Duration readTimeout) {

        return new RestClientBuilder()
            .url(url)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .build(ScenarioDesignAgentApi.class);
    }

    @Bean
    public PriceInterpretationAgentApi priceInterpretationAgentApi(@Value("${api.price-interpretation-agent.url:http://40.82.137.178:8003}") String url,
                                                                   @Value("${api.price-interpretation-agent.connection-timeout:1s}") Duration connectTimeout,
                                                                   @Value("${api.price-interpretation-agent.read-timeout:60s}") Duration readTimeout) {

        return new RestClientBuilder()
            .url(url)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .build(PriceInterpretationAgentApi.class);
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

    @Bean
    public RestClient fileCryptoRestClient(FileCryptoApiProperties properties) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectionTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        requestFactory.setConnectionRequestTimeout(properties.getReadTimeout());

        return RestClient.builder()
            .requestFactory(requestFactory)
            .defaultStatusHandler(
                HttpStatusCode::isError,
                (request, response) -> {
                    throw com.sk.common.exception.ApiErrorException.of(
                        response.getStatusCode().value(),
                        readBody(response.getBody()));
                })
            .messageConverters(converters ->
                converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                    RestClientDefaultConfig.httpObjectMapper())))
            .build();
    }

    private String readBody(java.io.InputStream body) throws IOException {
        return IOUtils.toString(body, StandardCharsets.UTF_8);
    }
}
