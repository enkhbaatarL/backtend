package com.sk.domain.mi.dto.response.marketData;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MarketDataChartSettingRes(
        @Schema(description = "마켓 데이터 설정 번호", example = "1") Integer marketDataSettingNo, // 마켓 데이터 설정 번호

        @Schema(description = "차트 표시 순위", example = "1") Short chartMarkRank, // 차트 표시 순위

        @Schema(description = "주요 상품 데이터 소스", example = "1") GoodsDataSourceDetail mainGoodsDataSource, // 주요 상품 데이터 소스

        @Schema(description = "주요 상품 데이터", example = "1") GoodsData mainGoodsData, // 주요 상품 데이터

        @Schema(description = "비교 상품 데이터 소스", example = "1") GoodsDataSourceDetail compareGoodsDataSource // 비교 상품 데이터 소스 (null 가능)
) {
    @QueryProjection
    public MarketDataChartSettingRes(
            Integer marketDataSettingNo,
            Short chartMarkRank,
            GoodsDataSourceDetail mainGoodsDataSource,
            GoodsData mainGoodsData,
            GoodsDataSourceDetail compareGoodsDataSource) {
        this.marketDataSettingNo = marketDataSettingNo;
        this.chartMarkRank = chartMarkRank;
        this.mainGoodsDataSource = mainGoodsDataSource;
        this.mainGoodsData = mainGoodsData;
        this.compareGoodsDataSource = compareGoodsDataSource;
    }
}
