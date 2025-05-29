package com.sk.domain.admin.dto.request.dynamicData;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

public record DynamicDataReq(
    @NotEmpty @Schema(description = "데이터 소스 번호", example = "1") String targetDataSource,
    @NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
    @NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
    @Schema(description = "조건 절", example = "id = 1") String whereClause,
    @Schema(description = "추가 조건 절", example = "id = 1") String additionalWhereClause,
    @Schema(description = "정렬 기준", example = "order by date") String orderByColumn,
    @Schema(description = "집계 기준", example = "group by date") String groupByColumn,
    @Schema(description = "조회 건수", example = "10") int limit,
    @Schema(description = "오프셋", example = "0") int offset
) {

    @Builder
    public DynamicDataReq(String targetDataSource, String columns, String tableName, String whereClause, String additionalWhereClause, String orderByColumn, String groupByColumn, int limit, int offset) {
        this.targetDataSource = targetDataSource;
        this.columns = columns;
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.additionalWhereClause = additionalWhereClause;
        this.orderByColumn = orderByColumn;
        this.groupByColumn = groupByColumn;
        this.limit = limit;
        this.offset = offset;
    }

    public static DynamicDataReq of(GoodsDataSourceDetail gds, String selectClause, String additionalWhereClause, String orderByColumn) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(selectClause)
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .orderByColumn(orderByColumn)
            .build();
    }

    public static DynamicDataReq of(GoodsDataSourceDetail gds, String additionalWhereClause, String orderByColumn) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(gds.getTableColumnListNm())
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .orderByColumn(orderByColumn)
            .build();
    }

    public static DynamicDataReq ofCountQuery(GoodsDataSourceDetail gds, String additionalWhereClause) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns("count(*)")
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .build();
    }

    public static DynamicDataReq ofPagingQuery(GoodsDataSourceDetail gds, String additionalWhereClause, String orderByColumn, int page, int size) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(gds.getTableColumnListNm())
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .orderByColumn(orderByColumn)
            .limit(size)
            .offset((page - 1) * size)
            .build();
    }

    public static DynamicDataReq ofMaxQuery(GoodsDataSourceDetail gds, String selectClause) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(selectClause)
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .build();
    }

    public static DynamicDataReq ofGroupByQuery(GoodsDataSourceDetail gds, String selectClause, String groupByColumn) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(selectClause)
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .groupByColumn(groupByColumn)
            .build();
    }

}
