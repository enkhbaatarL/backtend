package com.sk.domain.shared.dto.request.rpa;

import com.sk.domain.shared.enums.ComponentType;

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

        @Schema(description = "추가 데이터", example = "date=1234")
        String addData,

        @Schema(description = "엑셀테이블컬럼목록명", example = "po,co,ph,ad,aavt")
        String excelTableColumnListNm,

        @Schema(description = "상품데이터소스단위명", example = "kg")
        String goodsDataSourceUnitNm,

        @Schema(description = "상품데이터소스통화단위명", example = "USD")
        String goodsDataSourceCurrencyUnitNm,

        @Schema(description = "캔들차트가능여부", example = "true")
        Boolean isCandleChartPossible,

        @Schema(description = "변환대상상세유형명", example = "캔들차트")
        String convertTargetDetailTypeNm,

        @Schema(description = "데이터구조변환명", example = "GENERAL")
        ComponentType dataStructureConvertNm
    ) {}
}
