package com.sk.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NetworkConfig {

    @PostConstruct
    public void init() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }
}
