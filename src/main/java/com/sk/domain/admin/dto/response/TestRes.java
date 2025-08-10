package com.sk.domain.admin.dto.response;

import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record TestRes() {
    // getUserInfo에 대한 응답
    public record GetUserInfo(
            @Schema(description = "사용자 이름", example = "장민규") String name,
            @Schema(description = "이메일 주소", example = "mk.jang@twolinecode.com") String email
    ) {
        @Builder
        public GetUserInfo(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public static GetUserInfo of(TestReq.getUserInfo testReq) {
            return GetUserInfo.builder()
                    .name(testReq.name())
                    .email(testReq.email())
                    .build();
        }
    }

    // saveComponent에 대한 응답
    public record SaveComponent(
            @Schema(description = "컴포넌트유형ID", example = "comp123") String componentTypeId,
            @Schema(description = "컴포넌트유형명", example = "2025-04-03T12:00:00Z") String componentTypeNm,
            @Schema(description = "상품데이터소스명", example = "2025-04-03T12:00:00Z") String goodsDataSourceNm,
            @Schema(description = "대상데이터베이스명", example = "2025-04-03T12:00:00Z") String targetDbNm,
            @Schema(description = "대상테이블명", example = "2025-04-03T12:00:00Z") String targetTableNm,
            @Schema(description = "쿼리조건내용", example = "2025-04-03T12:00:00Z") String queryConditionCntt,
            @Schema(description = "테이블컬럼목록명", example = "2025-04-03T12:00:00Z") String tableColumnListNm,
            @Schema(description = "엑셀컬럼목록명", example = "2025-04-03T12:00:00Z") String excelColumnListNm,
            @Schema(description = "엑셀조건내용", example = "2025-04-03T12:00:00Z") String excelConditionCntt
    ) {
        @Builder
        public SaveComponent(String componentTypeId, String componentTypeNm, String goodsDataSourceNm, String targetDbNm, String targetTableNm, String queryConditionCntt, String tableColumnListNm, String excelColumnListNm, String excelConditionCntt) {
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

        public static SaveComponent of(ComponentInfo componentInfo) {
            return SaveComponent.builder()
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
    
}