package com.sk.common.utils.Properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// @ConfigurationProperties(prefix = "spring.datasource")
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {

    // private Map<String, DatasourceConfig> datasources = new HashMap<>();

    private MasterConfig master; // master 데이터 소스 설정
    private Map<String, TenantConfig> tenantDatasources = new HashMap<>(); // tenant-datasources 설정

    /**
     * Master 데이터 소스 설정
     */
    @Getter
    @Setter
    public static class MasterConfig {
        private String jdbcUrl;
        private String driverClassName;
        private String username;
        private String password;
        private HikariConfig hikari;
    }

    /**
     * Tenant 데이터(master 이외에 DB) 소스 설정
     */
    @Getter
    @Setter
    public static class TenantConfig {
        private String jdbcUrl;
        private String driverClassName;
        private String username;
        private String password;
        private HikariConfig hikari;
    }

    /**
     * Hikari 설정
     */
    @Getter
    @Setter
    public static class HikariConfig {
        private int maximumPoolSize;
        private long maxLifetime;
    }
}
