package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.enums.GoodsAuthCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GoodsAuthorityRes(

    @Schema(description = "상품번호", example = "12")
    Integer goodsNo,

    @Schema(description = "상품코드", example = "CD159")
    String goodsCd,

    @Schema(description = "보기권한 여부", example = "ture")
    boolean isViewable,

    @Schema(description = "다운로드권한 여부", example = "false")
    boolean isDownloadable

) {

    @Builder
    public GoodsAuthorityRes(Integer goodsNo, String goodsCd, boolean isViewable, boolean isDownloadable) {
        this.goodsNo = goodsNo;
        this.goodsCd = goodsCd;
        this.isViewable = isViewable;
        this.isDownloadable = isDownloadable;
    }

    public static GoodsAuthorityRes of(AuthManageGoodsMapping goodsMappings) {
        return GoodsAuthorityRes.builder()
            .goodsNo(goodsMappings.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo())
            .goodsCd(goodsMappings.getCustomerCompanyGoodsMapping().getGoods().getGoodsCd())
            .isViewable(GoodsAuthCd.VIEW == goodsMappings.getGoodsAuthCd())
            .isDownloadable(GoodsAuthCd.DOWNLOAD == goodsMappings.getGoodsAuthCd())
            .build();
    }

    public static List<GoodsAuthorityRes> fromAuthorityMappings(List<AuthManageGoodsMapping> goodsMappings) {

        Map<Integer, List<AuthManageGoodsMapping>> groupedByGoodsNo = goodsMappings.stream()
            .collect(Collectors.groupingBy(mapping ->
                mapping.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo()));

        return groupedByGoodsNo.entrySet().stream()
            .map(entry -> {
                Integer goodsNo = entry.getKey();
                List<AuthManageGoodsMapping> mappingsForGoods = entry.getValue();

                String goodsCd = mappingsForGoods.get(0).getCustomerCompanyGoodsMapping().getGoods().getGoodsCd();

                boolean isView = false;
                boolean isDownload = false;

                for (AuthManageGoodsMapping mapping : mappingsForGoods) {
                    GoodsAuthCd authCd = mapping.getGoodsAuthCd();
                    if (authCd == GoodsAuthCd.VIEW) isView = true;
                    if (authCd == GoodsAuthCd.DOWNLOAD) isDownload = true;
                    if (isView && isDownload) break;
                }

                return GoodsAuthorityRes.builder()
                    .goodsNo(goodsNo)
                    .goodsCd(goodsCd)
                    .isViewable(isView)
                    .isDownloadable(isDownload)
                    .build();
            })
            .toList();

    }

}
