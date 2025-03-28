package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record DynamicDataReq(
	@NotEmpty @Schema(description = "컬럼 명", example = "id, name") String columns,
	@NotEmpty @Schema(description = "테이블 명", example = "user") String tableName,
	@Schema(description = "조건 절", example = "id = 1") String whereClause
) {
}