package com.sk.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "server.provider.default")
public class ProviderDefaultProperties {
    private Map<Integer, String> languages;
}
