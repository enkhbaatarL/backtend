package com.sk.common.config.database;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.properties.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceFactory {

    private final DataSourceProperties dataSourceProperties;

    @Primary
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        DataSourceProperties.DataSourceConfig masterConfig = dataSourceProperties.getMaster();
        return createDataSource(masterConfig);
    }

    @Bean(name = "tenantDataSource")
    public DataSource tenantDatasource() {
        AbstractRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();

        List<DataSourceProperties.DataSourceConfig> tenantsConfig = dataSourceProperties.getTenants();

        if (tenantsConfig == null || tenantsConfig.isEmpty()) {
            throw SystemErrorException.of(ResultCode.Error.DB_CONNECTION_ERROR, "No datasources defined in configuration");
        }

        for (DataSourceProperties.DataSourceConfig config : tenantsConfig) {
            targetDataSources.put(config.getCompanyCd(), createDataSource(config));
        }

        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
        dynamicRoutingDataSource.setDefaultTargetDataSource(targetDataSources.get(targetDataSources.keySet().iterator().next()));
        return dynamicRoutingDataSource;
    }

    private DataSource createDataSource(DataSourceProperties.DataSourceConfig dataSourceConfig) {
        HikariDataSource hikariDataSource = new HikariDataSource();

        // HikariCP 기본 설정
        hikariDataSource.setJdbcUrl(dataSourceConfig.getJdbcUrl());
        hikariDataSource.setUsername(dataSourceConfig.getUsername());
        hikariDataSource.setPassword(dataSourceConfig.getPassword());
        hikariDataSource.setDriverClassName(dataSourceConfig.getDriverClassName());

        // HikariCP 추가 설정
        hikariDataSource.setConnectionTimeout(dataSourceConfig.getHikari().getConnectionTimeout().toMillis());
        hikariDataSource.setMaximumPoolSize(dataSourceConfig.getHikari().getMaximumPoolSize());
        hikariDataSource.setIdleTimeout(dataSourceConfig.getHikari().getIdleTimeout().toMillis());
        hikariDataSource.setMaxLifetime(dataSourceConfig.getHikari().getMaxLifetime().toMillis());
        hikariDataSource.setPoolName("%s-%s".formatted(dataSourceConfig.getCompanyCd().toLowerCase(), "pool"));

        // 자동 재연결 설정
        if (dataSourceConfig.getHikari().getConnectionTestQuery() != null) {
            hikariDataSource.setConnectionTestQuery(dataSourceConfig.getHikari().getConnectionTestQuery());
        }
        if (dataSourceConfig.getHikari().getValidationTimeout() != null) {
            hikariDataSource.setValidationTimeout(dataSourceConfig.getHikari().getValidationTimeout().toMillis());
        }
        if (dataSourceConfig.getHikari().getMinimumIdle() > 0) {
            hikariDataSource.setMinimumIdle(dataSourceConfig.getHikari().getMinimumIdle());
        }

        return hikariDataSource;
    }

}
