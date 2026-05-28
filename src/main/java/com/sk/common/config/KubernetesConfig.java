package com.sk.common.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        // 로컬: ~/.kube/config 자동으로 읽어서 연결
        // 운영(pod 내부): in-cluster 설정 자동 감지
        Config config = new ConfigBuilder()
            .withConnectionTimeout(5_000)
            .withRequestTimeout(15_000)
            .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
