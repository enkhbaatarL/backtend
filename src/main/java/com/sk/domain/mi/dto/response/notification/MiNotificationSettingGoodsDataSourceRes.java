package com.sk.domain.mi.dto.response.notification;

import com.querydsl.core.annotations.QueryProjection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiNotificationSettingGoodsDataSourceRes(

        @Schema(description = "상품번호", example = "1") Integer goodsNo,

        @Schema(description = "상품데이터번호", example = "1") Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "상품데이터명") String goodsDataNm,

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo,

        @Schema(description = "상품데이터소스명", example = "상품데이터소스명") String goodsDataSourceNm,

        @Schema(description = "설정알람수", example = "100") Integer settingNotificationCount) {

    @Builder
    @QueryProjection
    public MiNotificationSettingGoodsDataSourceRes(Integer goodsNo, Integer goodsDataNo, String goodsDataNm,
            Integer goodsDataSourceNo, String goodsDataSourceNm, Integer settingNotificationCount) {
        this.goodsNo = goodsNo;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.settingNotificationCount = settingNotificationCount;
    }
}
