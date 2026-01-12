package com.sk.domain.mi.dto.response.insights;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderCategoryRes(
    @Schema(description = "코드ID", example = "A_BLOOMBERG")
    String codeId,

    @Schema(description = "코드명", example = "Bloomberg")
    String codeNm,

    @Schema(description = "코드설명", example = "")
    String codeExpn,

    @Schema(description = "선택여부", example = "true")
    Boolean isChecked
) {
    /**
     * GoodsDataCategoryRes에서 ProviderCategoryRes로 변환
     *
     * @param category GoodsDataCategoryRes 객체
     * @param isChecked 선택 여부
     * @return ProviderCategoryRes 객체
     */
    public static ProviderCategoryRes of(
        com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes category,
        Boolean isChecked
    ) {
        return new ProviderCategoryRes(
            category.codeId(),
            category.codeNm(),
            category.codeExpn(),
            isChecked
        );
    }

    /**
     * GoodsDataCategoryRes에서 ProviderCategoryRes로 변환 (isChecked 기본값: false)
     *
     * @param category GoodsDataCategoryRes 객체
     * @return ProviderCategoryRes 객체
     */
    public static ProviderCategoryRes of(
        com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes category
    ) {
        return of(category, false);
    }
}
