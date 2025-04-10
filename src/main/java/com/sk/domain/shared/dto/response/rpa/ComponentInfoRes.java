package com.sk.domain.shared.dto.response.rpa;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ComponentInfoRes(

    @Schema(description = "컴포넌트유형ID", example = "TYPEID")
    String componentTypeId,

    @Schema(description = "컴포넌트유형명", example = "컴포넌트유형명")
    String componentTypeNm,

    @Schema(description = "상품데이터소스명", example = "상품데이터소스명")
    String goodsDataSourceNm,

    @Schema(description = "대상데이터베이스명", example = "mi")
    String targetDbNm,

    @Schema(description = "대상테이블명", example = "tif_tabe_nm")
    String targetTableNm,

    @Schema(description = "쿼리조건내용", example = "1 = 1")
    String queryConditionCntt,

    @Schema(description = "테이블컬럼목록명", example = "po,co,ph,ad,aavt")
    String tableColumnListNm,

    @Schema(description = "엑셀컬럼목록명", example = "po,co,ph,ad,aavt")
    String excelColumnListNm,

    @Schema(description = "엑셀조건내용", example = "date >= '2024-01-01'")
    String excelConditionCntt
) {

    @Builder
    public ComponentInfoRes(String componentTypeId, String componentTypeNm, String goodsDataSourceNm, String targetDbNm, String targetTableNm, String queryConditionCntt, String tableColumnListNm, String excelColumnListNm, String excelConditionCntt) {
        this.componentTypeId = componentTypeId;
        this.componentTypeNm = componentTypeNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.targetDbNm = targetDbNm;
        this.targetTableNm = targetTableNm;
        this.queryConditionCntt = queryConditionCntt;
        this.tableColumnListNm = tableColumnListNm;
        this.excelColumnListNm = excelColumnListNm;
        this.excelConditionCntt = excelConditionCntt;
    }

    public static ComponentInfoRes of(ComponentInfo componentInfo) {
        return ComponentInfoRes.builder()
            .componentTypeId(componentInfo.getComponentTypeId())
            .componentTypeNm(componentInfo.getComponentTypeNm())
            .goodsDataSourceNm(componentInfo.getGoodsDataSourceNm())
            .targetDbNm(componentInfo.getTargetDbNm())
            .targetTableNm(componentInfo.getTargetTableNm())
            .queryConditionCntt(componentInfo.getQueryConditionCntt())
            .tableColumnListNm(componentInfo.getTableColumnListNm())
            .excelColumnListNm(componentInfo.getExcelColumnListNm())
            .excelConditionCntt(componentInfo.getExcelConditionCntt())
            .build();
    }
}
