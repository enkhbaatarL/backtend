package com.sk.common.http;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RestClientBuilder {

    public static final Duration DEFAULT_CONNECTION_TIMEOUT_DURATION = Duration.ofMillis(1000L);
    public static final Duration DEFAULT_READ_TIMEOUT_DURATION = Duration.ofMillis(5000L);

    private String url;
    private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_DURATION;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT_DURATION;
    /** 같은 호스트 동시 연결 수. null 이면 HttpClient 기본 풀(호스트당 5, 전체 25)을 쓴다. */
    private Integer maxConnections;

    private final List<ClientHttpRequestInterceptor> initRequestInterceptor = new ArrayList<>();

    private final Map<String, String> headers = new HashMap<>();

    public RestClientBuilder url(String url) {
        this.url = url;
        return this;
    }

    public RestClientBuilder connectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public RestClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * 동시 연결 수를 지정한다 (호스트당·전체 모두 이 값). 응답이 오래 걸리는 API 를 여러 스레드가 동시에 부를 때,
     * 기본 풀(호스트당 5)에 막혀 연결을 기다리지 않도록 호출 측 동시 실행 수에 맞춘다.
     */
    public RestClientBuilder maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public RestClientBuilder requestInterceptor(ClientHttpRequestInterceptor requestInterceptor){
        this.initRequestInterceptor.add(requestInterceptor);
        return this;
    }

    public RestClientBuilder headers(String... keyValuePairs) {

        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs should be provided in pairs.");
        }

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[i + 1];
            headers.put(key, value);
        }

        return this;
    }

    public <T> T build(Class<T> clientClass) {

        HttpComponentsClientHttpRequestFactory requestFactory = maxConnections == null
            ? new HttpComponentsClientHttpRequestFactory()
            : new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
                .useSystemProperties()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                    .setMaxConnPerRoute(maxConnections)
                    .setMaxConnTotal(maxConnections)
                    .build())
                .build());
        requestFactory.setConnectTimeout(connectionTimeout);
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.setConnectionRequestTimeout(readTimeout);

        Builder restClientBuilder = RestClientDefaultConfig.defaultRestClientBuilder()
            .baseUrl(url)
            .requestFactory(requestFactory);

        if(!this.initRequestInterceptor.isEmpty()){
            for(ClientHttpRequestInterceptor interceptor : this.initRequestInterceptor){
                restClientBuilder.requestInterceptor(interceptor);
            }
        }
        RestClient restClient = restClientBuilder.build();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            restClient = restClient.mutate().defaultHeader(entry.getKey(), entry.getValue()).build();
        }

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }

}
