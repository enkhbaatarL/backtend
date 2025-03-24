package com.sk.common.config.database.dataSource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.sk.common.utils.Properties.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

/**
 * Master를 제외한 다른 테넌트 사용하는 데이터소스 설정
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DataSourceProperties.class)
public class DynamicDataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.max-lifetime}")
    private int maxLifetime;

    @Bean
    public DataSource dynamicDataSource() {
        AbstractRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();

        Map<String, DataSourceProperties.DatasourceConfig> datasources = dataSourceProperties.getDatasources();
        if (datasources == null || datasources.isEmpty()) {
            throw new IllegalStateException("No datasources defined in configuration");
        }

        for (Map.Entry<String, DataSourceProperties.DatasourceConfig> entry : datasources.entrySet()) {
            String key = entry.getKey();
            DataSourceProperties.DatasourceConfig config = entry.getValue();
            targetDataSources.put(key, createDataSource(
                    config.getJdbcUrl(),
                    config.getUsername(),
                    config.getPassword(),
                    config.getDriverClassName()));
        }

        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
        dynamicRoutingDataSource.setDefaultTargetDataSource(targetDataSources.get("master"));
        return dynamicRoutingDataSource;
    }

    private DataSource createDataSource(String url, String username, String password, String driverClassName) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        hikariDataSource.setDriverClassName(driverClassName);
        // yml의 hikari 설정 반영
        hikariDataSource.setMaximumPoolSize(maximumPoolSize);
        hikariDataSource.setMaxLifetime(maxLifetime);
        return hikariDataSource;
    }
}