package com.sk.common.config.database.dataSource;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private Map<Object, Object> customTargetDataSources = new HashMap<>();

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.customTargetDataSources = targetDataSources; // 내부 맵 동기화
    }

    public Map<Object, Object> getTargetDataSources() {
        return new HashMap<>(customTargetDataSources); // 방어적 복사본 반환
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceKey();
    }
}
