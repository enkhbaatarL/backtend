package com.sk.common.config.database.dataSource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.sk.common.utils.Properties.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

/**
 * Master를 제외한 다른 테넌트 데이터소스 설정
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DataSourceProperties.class)
public class DynamicDataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    @Bean
    public DataSource dynamicDataSource() {
        AbstractRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();

        Map<String, DataSourceProperties.TenantConfig> datasources = dataSourceProperties.getTenantDatasources();
        if (datasources == null || datasources.isEmpty()) {
            throw new IllegalStateException("No datasources defined in configuration");
        }

        for (Map.Entry<String, DataSourceProperties.TenantConfig> entry : datasources.entrySet()) {
            String key = entry.getKey();
            DataSourceProperties.TenantConfig config = entry.getValue();
            targetDataSources.put(key, createDataSource(
                    config.getJdbcUrl(),
                    config.getUsername(),
                    config.getPassword(),
                    config.getDriverClassName(),
                    config.getHikari().getMaximumPoolSize(),
                    config.getHikari().getMaxLifetime()));
        }

        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
        dynamicRoutingDataSource.setDefaultTargetDataSource(targetDataSources.get("hynix"));
        return dynamicRoutingDataSource;
    }

    private DataSource createDataSource(String url, String username, String password, String driverClassName,
            int maximumPoolSize, long maxLifetime) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        hikariDataSource.setDriverClassName(driverClassName);
        hikariDataSource.setMaximumPoolSize(maximumPoolSize);
        hikariDataSource.setMaxLifetime(maxLifetime);
        return hikariDataSource;
    }
}
