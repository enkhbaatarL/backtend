package com.sk.domain.shared.client.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.sk.common.http.RestClientDefaultConfig;
import com.sk.domain.shared.client.EventVerificationAgentApi;
import com.sk.domain.shared.client.EventVerificationUpdateAgentApi;
import java.time.Duration;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class EventVerificationClientConfig {
    @Bean(destroyMethod = "close")
    public CloseableHttpClient eventVerificationHttpClient() {
        // 재검증은 새 웹 검색/LLM 호출이므로 자동 재시도하지 않는다.
        return HttpClients.custom().useSystemProperties().disableAutomaticRetries().disableRedirectHandling().build();
    }

    @Bean
    public EventVerificationAgentApi eventVerificationAgentApi(
        @Qualifier("eventVerificationHttpClient") CloseableHttpClient httpClient,
        @Value("${api.event-verification-agent.url:${api.scenario-design-agent.url:http://localhost:8000}}") String url,
        @Value("${api.event-verification-agent.connection-timeout:3s}") Duration connectionTimeout,
        @Value("${api.event-verification-agent.read-timeout:210s}") Duration readTimeout) {
        return proxyFactory(httpClient, url, connectionTimeout, readTimeout).createClient(EventVerificationAgentApi.class);
    }

    @Bean
    public EventVerificationUpdateAgentApi eventVerificationUpdateAgentApi(
        @Qualifier("eventVerificationHttpClient") CloseableHttpClient httpClient,
        @Value("${api.event-verification-agent.url:${api.scenario-design-agent.url:http://localhost:8000}}") String url,
        @Value("${api.event-verification-agent.connection-timeout:3s}") Duration connectionTimeout,
        @Value("${api.event-verification-agent.update-read-timeout:420s}") Duration readTimeout) {
        return proxyFactory(httpClient, url, connectionTimeout, readTimeout).createClient(EventVerificationUpdateAgentApi.class);
    }

    private HttpServiceProxyFactory proxyFactory(CloseableHttpClient httpClient, String url,
                                                Duration connectionTimeout, Duration readTimeout) {
        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectionTimeout);
        factory.setConnectionRequestTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        var mapper = RestClientDefaultConfig.httpObjectMapper().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        var client = RestClient.builder().baseUrl(url.replaceAll("/+$", ""))
            .requestFactory(factory)
            .messageConverters(converters -> {
                converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                converters.add(new MappingJackson2HttpMessageConverter(mapper));
            }).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build();
    }
}
