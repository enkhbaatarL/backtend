package com.sk.domain.admin.dto.request;

import java.util.List;

import lombok.Builder;

public record DynamicDataSaveAll(
    String tartgetDataSource,
    String tableName,
    List<String> columns,
    List<Object[]> values
) {
    @Builder
    public DynamicDataSaveAll(String tartgetDataSource, String tableName, List<String> columns, List<Object[]> values) {
        this.tartgetDataSource = tartgetDataSource;
        this.tableName = tableName;
        this.columns = columns;
        this.values = values;
    }

    public static DynamicDataSaveAll of(String tartgetDataSource, String tableName, List<String> columns, List<Object[]> values) {
        return DynamicDataSaveAll.builder()
            .tartgetDataSource(tartgetDataSource)
            .tableName(tableName)
            .columns(columns)
            .values(values)
            .build();
    }
}

