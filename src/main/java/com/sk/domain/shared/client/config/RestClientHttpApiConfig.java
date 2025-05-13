package com.sk.domain.shared.client.config;

import com.sk.common.http.RestClientBuilder;
import com.sk.domain.shared.client.ModelAgentApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RestClientHttpApiConfig {

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
                                       @Value("${api.model-agent.read-timeout:10s}") Duration readTimeout) {

        return new RestClientBuilder()
            .url(url)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .build(ModelAgentApi.class);
    }

}
