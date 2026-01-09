package com.sk.domain.mi.dto.response.insights;

import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import java.util.List;

public record GoodsDataFilterRes(
    List<GoodsDataCategoryRes> documentTypes,
    List<ProviderCategoryRes> providers,
    List<GoodsDataCategoryRes> languages) {

    public GoodsDataFilterRes(List<GoodsDataCategoryRes> documentTypes,
        List<ProviderCategoryRes> providers,
        List<GoodsDataCategoryRes> languages) {
        this.documentTypes = documentTypes;
        this.providers = providers;
        this.languages = languages;
    }

    public static GoodsDataFilterRes of(List<GoodsDataCategoryRes> documentTypes,
        List<ProviderCategoryRes> providers,
        List<GoodsDataCategoryRes> languages) {
        return new GoodsDataFilterRes(documentTypes, providers, languages);
    }
}
