package com.sk.common.config.database.dataSource;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.sk.common.utils.Properties.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

/**
 * master 데이터소스 설정
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@RequiredArgsConstructor
public class MasterDataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    @Bean(name = "masterDataSource")
    @Primary
    public DataSource masterDataSource() {
        DataSourceProperties.MasterConfig masterConfig = dataSourceProperties.getMaster();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(masterConfig.getJdbcUrl());
        dataSource.setDriverClassName(masterConfig.getDriverClassName());
        dataSource.setUsername(masterConfig.getUsername());
        dataSource.setPassword(masterConfig.getPassword());
        dataSource.setMaximumPoolSize(masterConfig.getHikari().getMaximumPoolSize());
        dataSource.setMaxLifetime(masterConfig.getHikari().getMaxLifetime());
        return dataSource;
    }
}
