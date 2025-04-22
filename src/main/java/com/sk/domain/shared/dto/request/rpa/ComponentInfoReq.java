package com.sk.domain.shared.dto.request.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ComponentInfoReq() {
    public record Insert(

        @NotNull
        @Schema(description = "컴포넌트유형ID", example = "TYPEID")
        String componentTypeId,

        @NotNull
        @Schema(description = "컴포넌트유형명", example = "컴포넌트유형명")
        String componentTypeNm,

        @NotNull
        @Schema(description = "상품데이터소스명", example = "상품데이터소스명")
        String goodsDataSourceNm,

        @NotNull
        @Schema(description = "대상데이터베이스명", example = "mi")
        String targetDbNm,

        @NotNull
        @Schema(description = "대상테이블명", example = "tif_tabe_nm")
        String targetTableNm,

        @Schema(description = "쿼리조건내용", example = "1 = 1")
        String queryConditionCntt,

        @Schema(description = "테이블컬럼목록명, 컴포넌트에서 테이블 Select : 순서에 따라 그래프로 그리기 위한 컬럼 : x축, y축_1, y축_2, y축_3, y축_4", example = "po,co,ph,ad,aavt")
        String tableColumnListNm,

        @Schema(description = "엑셀컬럼목록명", example = "po,co,ph,ad,aavt")
        String excelColumnListNm,

        @Schema(description = "엑셀조건내용", example = "date >= '2024-01-01'")
        String excelConditionCntt,

        @Schema(description = "상품데이터소스단위명", example = "kg")
        String goodsDataSourceUnitNm
    ) {}
}
