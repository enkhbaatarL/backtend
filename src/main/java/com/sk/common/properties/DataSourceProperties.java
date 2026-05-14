package com.sk.common.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {

    private DataSourceConfig master;
    private List<DataSourceConfig> tenants = new ArrayList<>();

    @Getter
    @Setter
    public static class DataSourceConfig {
        private String companyCd;
        private String jdbcUrl;
        private String driverClassName;
        private String username;
        private String password;
        private HikariConfig hikari;
    }

    @Getter
    @Setter
    public static class HikariConfig {
        private Duration connectionTimeout;
        private int maximumPoolSize;
        private Duration maxLifetime;
        private Duration idleTimeout;
        private String connectionTestQuery;
        private Duration validationTimeout;
        private int minimumIdle;
        private Duration keepaliveTime;
    }

}
