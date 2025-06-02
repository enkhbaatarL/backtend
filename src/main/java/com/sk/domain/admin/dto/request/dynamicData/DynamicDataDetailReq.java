package com.sk.domain.admin.dto.request.dynamicData;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

public record DynamicDataDetailReq(
        @NotEmpty @Schema(description = "데이터 소스 번호", example = "1") String targetDataSource,
        @NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
        @NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
        @Schema(description = "조건 절", example = "id = 1") String whereClause,
        @Schema(description = "내부 조인 조건", example = "id = 1") String innerJoin,
        @Schema(description = "정렬 컬럼", example = "id, name") String orderByColumns,
        @Schema(description = "제한 조건", example = "10") String limit) {

    @Builder
    public DynamicDataDetailReq(String targetDataSource, String tableName, String columns, String whereClause, String innerJoin,
            String orderByColumns, String limit) {
        this.targetDataSource = targetDataSource;
        this.tableName = tableName;
        this.columns = columns;
        this.whereClause = whereClause;
        this.innerJoin = innerJoin;
        this.orderByColumns = orderByColumns;
        this.limit = limit;
    }

    public static DynamicDataDetailReq of(String targetDataSource, String tableName, String columns, String whereClause) {
        return DynamicDataDetailReq.builder()
                .targetDataSource(targetDataSource)
                .tableName(tableName)
                .columns(columns)
                .whereClause(whereClause)
                .build();
    }

    public static DynamicDataDetailReq ofOrderBy(String targetDataSource, String tableName, String columns, String whereClause,
            String orderByColumns) {
        return DynamicDataDetailReq.builder()
                .targetDataSource(targetDataSource)
                .tableName(tableName)
                .columns(columns)
                .whereClause(whereClause)
                .orderByColumns(orderByColumns)
                .build();
    }

    public static DynamicDataDetailReq ofLimit(String targetDataSource, String tableName, String columns, String whereClause, String limit) {
        return DynamicDataDetailReq.builder()
                .targetDataSource(targetDataSource)
                .tableName(tableName)
                .columns(columns)
                .whereClause(whereClause)
                .limit(limit)
                .build();
    }

    public static DynamicDataDetailReq ofInnerJoin(String targetDataSource, String tableName, String columns, String innerJoin,
            String orderByColumns) {
        return DynamicDataDetailReq.builder()
                .targetDataSource(targetDataSource)
                .tableName(tableName)
                .columns(columns)
                .innerJoin(innerJoin)
                .orderByColumns(orderByColumns)
                .build();
    }
}
