package com.sk.domain.admin.dto.request;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

public record DynamicDataReq(
	@NotEmpty @Schema(description = "데이터 소스 번호", example = "1") String tartgetDataSource,
    @NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
	@NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
	@Schema(description = "조건 절", example = "id = 1") String whereClause,
	@Schema(description = "추가 조건 절", example = "id = 1") String additionalWhereClause
) {

    @Builder
    public DynamicDataReq(String tartgetDataSource, String columns, String tableName, String whereClause, String additionalWhereClause) {
        this.tartgetDataSource = tartgetDataSource;
        this.columns = columns;
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.additionalWhereClause = additionalWhereClause;
    }

    public static DynamicDataReq of(GoodsDataSourceDetail goodsDataSourceDetail, String additionalWhere) {
        return DynamicDataReq.builder()
            .tartgetDataSource(goodsDataSourceDetail.getTargetDbNm())
            .columns(goodsDataSourceDetail.getTableColumnListNm())
            .tableName(goodsDataSourceDetail.getTargetTableNm())
            .whereClause(goodsDataSourceDetail.getQueryConditionCntt())
            .additionalWhereClause(additionalWhere)
            .build();
    }
}
