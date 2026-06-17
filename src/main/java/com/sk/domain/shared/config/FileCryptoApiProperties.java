package com.sk.domain.shared.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.file-crypto")
public class FileCryptoApiProperties {

    private String baseUrl;

    private String uploadPath;

    private String encryptPath;

    private String decryptPath;

    private Duration connectionTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(30);

    private int retryCount = 1;

    private long retryBackoffMs = 500L;

    private String pid;

    private String labelId;

    private String userId;
}
