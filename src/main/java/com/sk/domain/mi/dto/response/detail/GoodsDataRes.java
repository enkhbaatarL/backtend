package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record GoodsDataRes(

    @Schema(description = "상품 데이터 명", example = "Eye on the tiger...")
    String goodsDataNm,

    @Schema(description = "상품 데이터 설명", example = "데이터 설명")
    String goodsDataExpn,

    @Schema(description = "상품 데이터 요약", example = "데이터 요약")
    String goodsDataSummaryCntt,

    @Schema(description = "프로바이더 명", example = "Wood Mackenzie")
    String providerNm,

    @Schema(description = "카테고리 데이터 정보 목록")
    List<CategoryDataInfo> categoryDataInfo,

    @Schema(description = "보기 권한", example = "true")
    Boolean isViewable,

    @Schema(description = "다운로드 권한", example = "true")
    Boolean isDownloadable,

    AuditInfo auditInfo

) {

    @Builder
    public record CategoryDataInfo(

        @Schema(description = "데이터 정보 타입", example = "A_TYPES", allowableValues = {"A_TYPES", "A_LANGUAGES"})
        String type,

        @Schema(description = "데이터 정보 값", example = "Insight")
        String value

    ) {

        public static CategoryDataInfo of(GoodsDataCategoryProjection projection) {
            return CategoryDataInfo.builder()
                .type(projection.getCodeId())
                .value(projection.getCodeNm())
                .build();
        }

    }

    public static GoodsDataRes of(GoodsData entity, List<GoodsDataCategoryProjection> categoryProjections) {
        return GoodsDataRes.builder()
            .goodsDataNm(entity.getGoodsDataNm())
            .goodsDataExpn(entity.getGoodsDataExpn())
            .goodsDataSummaryCntt(entity.getGoodsDataSummaryCntt())
            .providerNm(entity.getGoods().getProvider().getProviderNm())
            .categoryDataInfo(categoryProjections.stream()
                .map(CategoryDataInfo::of)
                .toList())
            .isViewable(Boolean.TRUE)
            .isDownloadable(Boolean.TRUE)
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

}
