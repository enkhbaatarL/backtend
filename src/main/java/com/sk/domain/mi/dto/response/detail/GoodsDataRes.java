package com.sk.domain.mi.dto.response.detail;

import com.sk.common.utils.MDCUtil;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.enums.CategorySeparateHighCd;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Schema(description = "카테고리 정보 목록")
    List<CategoryDataInfo> categories,

    @Schema(description = "보기 권한", example = "true")
    Boolean isViewable,

    @Schema(description = "다운로드 권한", example = "true")
    Boolean isDownloadable,

    AuditInfo auditInfo

) {

    @Builder
    public record CategoryDataInfo(

        @Schema(
            description = "카테고리 상위코드",
            example = "LANGUAGES",
            allowableValues = {"LANGUAGES"}
        )
        CategorySeparateHighCd categoryHighCd,

        @ArraySchema(
            arraySchema = @Schema(description = "카테고리 값 목록"),
            schema = @Schema(description = "카테고리 값 요소", example = "ENGLISH")
        )
        List<String> value

    ) {

        public static List<CategoryDataInfo> ofList(List<GoodsDataCategoryProjection> projections) {
            Map<String, List<GoodsDataCategoryProjection>> grouped = projections.stream().collect(Collectors.groupingBy(GoodsDataCategoryProjection::getHighCodeId));
            return grouped.keySet().stream()
                .map(key -> CategoryDataInfo.builder()
                    .categoryHighCd(CategorySeparateHighCd.fromCodeId(key))
                    .value(grouped.get(key).stream().map(GoodsDataCategoryProjection::getCodeNm).toList())
                    .build())
                .toList();
        }

    }

    public static GoodsDataRes of(Integer goodsNo, GoodsData entity, List<GoodsDataCategoryProjection> categoryProjections, PermissionRes permissionRes, Integer priorityCompanyNo) {
        return GoodsDataRes.builder()
            .goodsDataNm(entity.getGoodsDataNm())
            .goodsDataExpn(entity.getGoodsDataExpn())
            .goodsDataSummaryCntt(entity.getGoodsDataSummaryCntt())
            .providerNm(entity.getGoods().getProvider().getProviderNm())
            .categories(CategoryDataInfo.ofList(categoryProjections))
            .isViewable(permissionRes.getGoodsViewableMap(priorityCompanyNo).getOrDefault(goodsNo, Boolean.FALSE))
            .isDownloadable(permissionRes.getGoodsDownloadableMap(priorityCompanyNo).getOrDefault(goodsNo, Boolean.FALSE))
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()))
            .build();
    }

}
