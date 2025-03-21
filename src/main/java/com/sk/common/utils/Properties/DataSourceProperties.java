package com.sk.common.utils.Properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {

    private Map<String, DatasourceConfig> datasources = new HashMap<>();

    @Getter
    @Setter
    public static class DatasourceConfig {
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName;
    }
}