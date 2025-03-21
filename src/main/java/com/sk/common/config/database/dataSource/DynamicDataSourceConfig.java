package com.sk.common.config.database.dataSource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

// import com.sk.domain.shared.entity.master.test.DataBaseSource;
// import com.sk.domain.shared.repository.master.test.DataBaseSourceRepository;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DynamicDataSourceConfig {

    @Value("${spring.datasource.master.jdbc-url}")
    private String masterJdbcUrl;

    @Value("${spring.datasource.master.username}")
    private String masterUsername;

    @Value("${spring.datasource.master.password}")
    private String masterPassword;

    @Value("${spring.datasource.master.driver-class-name}")
    private String masterDriverClassName;

    @Value("${spring.datasource.hynix.jdbc-url}")
    private String hynixJdbcUrl;

    @Value("${spring.datasource.hynix.username}")
    private String hynixUsername;

    @Value("${spring.datasource.hynix.password}")
    private String hynixPassword;

    @Value("${spring.datasource.hynix.driver-class-name}")
    private String hynixDriverClassName;

    @Bean
    public DataSource dynamicDataSource() {
        AbstractRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();

        targetDataSources.put("master", createDataSource(masterJdbcUrl, masterUsername, masterPassword,
                masterDriverClassName));

        targetDataSources.put("hynix", createDataSource(hynixJdbcUrl, hynixUsername, hynixPassword,
                hynixDriverClassName));

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
        return hikariDataSource;
    }
}