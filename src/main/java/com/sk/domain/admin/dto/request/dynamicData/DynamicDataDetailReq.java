package com.sk.domain.admin.dto.request.dynamicData;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

public record DynamicDataDetailReq(
	@NotEmpty @Schema(description = "데이터 소스 번호", example = "1") String tartgetDataSource,
	@NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
    @NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
	@Schema(description = "조건 절", example = "id = 1") String whereClause,
    @Schema(description = "정렬 컬럼", example = "id, name") String orderByColumns,
    @Schema(description = "제한 조건", example = "10") String limit
) {

    @Builder
    public DynamicDataDetailReq(String tartgetDataSource, String tableName, String columns, String whereClause, String orderByColumns, String limit) {
        this.tartgetDataSource = tartgetDataSource;
        this.tableName = tableName;
        this.columns = columns;
        this.whereClause = whereClause;
        this.orderByColumns = orderByColumns;
        this.limit = limit;
    }

    public static DynamicDataDetailReq ofOrderBy(String tartgetDataSource, String tableName, String columns, String whereClause, String orderByColumns) {
        return DynamicDataDetailReq.builder()
            .tartgetDataSource(tartgetDataSource)
            .tableName(tableName)
            .columns(columns)
            .whereClause(whereClause)
            .orderByColumns(orderByColumns)
            .build();
    }

    public static DynamicDataDetailReq ofLimit(String tartgetDataSource, String tableName, String columns, String whereClause, String limit) {
        return DynamicDataDetailReq.builder()
            .tartgetDataSource(tartgetDataSource)
            .tableName(tableName)
            .columns(columns)
            .whereClause(whereClause)
            .limit(limit)
            .build();
    }
}
