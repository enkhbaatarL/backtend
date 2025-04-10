package com.sk.domain.admin.dto.request;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

public record DynamicDataReq(
	@NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
	@NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
	@Schema(description = "조건 절", example = "id = 1") String whereClause
) {

    @Builder
    public DynamicDataReq(String columns, String tableName, String whereClause) {
        this.columns = columns;
        this.tableName = tableName;
        this.whereClause = whereClause;
    }

    public static DynamicDataReq of(GoodsDataSourceDetail goodsDataSourceDetail) {
        return DynamicDataReq.builder()
            .columns(goodsDataSourceDetail.getTableColumnListNm())
            .tableName(goodsDataSourceDetail.getTargetTableNm())
            .whereClause(goodsDataSourceDetail.getQueryConditionCntt())
            .build();
    }
}
