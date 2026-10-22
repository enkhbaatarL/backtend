package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record ModelTableNode(

    @Schema(description = "모델 시트 내 테이블명 (시스템 key — 저장/매칭용)", example = "table1")
    String tableName,

    @Schema(description = "테이블 화면 표시명 (카탈로그 매핑 불가 시 tableName 그대로)", example = "Annual Power Mix Adjustment")
    String displayTableName
) {
}
