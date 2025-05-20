package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Optional;

public record GoodsDataSourceRes(

        @Schema(description = "상품 데이터 소스 번호", example = "10004") Integer goodsDataSourceNo,

        @Schema(description = "상품 데이터 소스 명", example = "structured data 1234") String goodsDataSourceNm,

        @Schema(description = "데이터 최소 주기 (D: 일간, W: 주간, M: 월간, Y: 연간)", allowableValues = {
                "D", "W", "M", "Y"}, example = "D") PeriodUnitSeparateCd minInterval

    ){
    @Builder
    public GoodsDataSourceRes(
            Integer goodsDataSourceNo,
            String goodsDataSourceNm,
            PeriodUnitSeparateCd minInterval) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.minInterval = minInterval;
    }

    public static GoodsDataSourceRes of(GoodsDataSourceDetail entity) {
        return GoodsDataSourceRes.builder()
                .goodsDataSourceNo(entity.getGoodsDataSourceNo())
                .goodsDataSourceNm(entity.getGoodsDataSourceNm())
                .minInterval(Optional.ofNullable(entity.getPeriodUnitSeparateCd()).orElse(PeriodUnitSeparateCd.D))
                .build();
    }

    public static List<GoodsDataSourceRes> ofList(List<GoodsDataSourceDetail> entities) {
        return entities.stream()
                .map(GoodsDataSourceRes::of)
                .toList();
    }

}
