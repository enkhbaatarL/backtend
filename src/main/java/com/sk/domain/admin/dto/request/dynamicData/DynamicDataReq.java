package com.sk.domain.admin.dto.request.dynamicData;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Optional;

public record DynamicDataReq(
    @NotEmpty @Schema(description = "데이터 소스 번호", example = "1") String targetDataSource,
    @NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
    @NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
    @Schema(description = "조건 절", example = "id = 1") String whereClause,
    @Schema(description = "추가 조건 절", example = "id = 1") String additionalWhereClause,
    @Schema(description = "정렬 기준", example = "order by date") String orderBy,
    @Schema(description = "조회 건수", example = "10") int limit,
    @Schema(description = "오프셋", example = "0") int offset
) {

    @Builder
    public DynamicDataReq(String targetDataSource, String columns, String tableName, String whereClause, String additionalWhereClause, String orderBy, int limit, int offset) {
        this.targetDataSource = targetDataSource;
        this.columns = columns;
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.additionalWhereClause = additionalWhereClause;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
    }

    public static DynamicDataReq of(GoodsDataSourceDetail gds, String additionalWhereClause) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(gds.getTableColumnListNm())
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .build();
    }

    public static DynamicDataReq of(GoodsDataSourceDetail gds, String selectClause, String additionalWhereClause, String orderByColumn) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(selectClause)
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .orderBy(orderByColumn)
            .build();
    }

    public static DynamicDataReq of(GoodsDataSourceDetail gds, String additionalWhereClause, String orderByColumn) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(gds.getTableColumnListNm())
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .additionalWhereClause(additionalWhereClause)
            .orderBy(orderByColumn)
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
            .orderBy(orderByColumn)
            .limit(size)
            .offset((page - 1) * size)
            .build();
    }

    public static DynamicDataReq ofLatestDate(GoodsDataSourceDetail gds, String selectClause) {
        return DynamicDataReq.builder()
            .targetDataSource(gds.getTargetDbNm())
            .columns(selectClause)
            .tableName(gds.getTargetTableNm())
            .whereClause(gds.getQueryConditionCntt())
            .build();
    }

}
