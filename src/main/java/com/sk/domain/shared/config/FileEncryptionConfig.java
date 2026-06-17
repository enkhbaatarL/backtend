package com.sk.domain.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        FileEncryptionProperties.class,
        FileCryptoApiProperties.class
})
public class FileEncryptionConfig {
}
