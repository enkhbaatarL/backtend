package com.sk.common.http;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

    private List<ClientHttpRequestInterceptor> initRequestInterceptor = new ArrayList<>();

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

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
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
