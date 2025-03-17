package com.sk.domain.admin.dto.response.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.enums.GoodsAuthCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public record CompanyGoodsMapDetailRes(

    @JsonUnwrapped
    CompanyGoodsMapRes companyGoodsMapRes,

    @Schema(description = "상품 명", example = "제조사별 DRAM 생산량")
    String goodsNm,

    @Schema(description = "상품 설명", example = "설명")
    String goodsExpn,

    CompanySetting companySetting

) {

    public record CompanySetting(

        @Schema(description = "보기권한 여부", example = "ture")
        boolean isViewable,

        @Schema(description = "다운로드권한 여부", example = "false")
        boolean isDownloadable,

        @Schema(description = "사용시작 일자", example = "20250101")
        String usageBeginDate,

        @Schema(description = "사용종료 일자", example = "20251231")
        String usageEndDate

    ) {

        @Builder
        public CompanySetting(boolean isViewable, boolean isDownloadable, String usageBeginDate, String usageEndDate) {
            this.isViewable = isViewable;
            this.isDownloadable = isDownloadable;
            this.usageBeginDate = usageBeginDate;
            this.usageEndDate = usageEndDate;
        }

        public static CompanySetting of(List<AuthManageGoodsMapping> goodsMappings) {

            Map<Integer, List<AuthManageGoodsMapping>> groupedByGoodsNo = goodsMappings.stream()
                .collect(Collectors.groupingBy(mapping ->
                    mapping.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo()));

            AtomicBoolean isView = new AtomicBoolean(false);
            AtomicBoolean isDownload = new AtomicBoolean(false);
            AtomicReference<String> start = new AtomicReference<>();
            AtomicReference<String> end = new AtomicReference<>();

            groupedByGoodsNo.forEach((goodsNo, mappingsForGoods) -> {

                start.set(mappingsForGoods.get(0).getUseBeginDate());
                end.set(mappingsForGoods.get(0).getUseEndDate());

                for (AuthManageGoodsMapping mapping : mappingsForGoods) {
                    GoodsAuthCd authCd = mapping.getGoodsAuthCd();
                    if (authCd == GoodsAuthCd.VIEW) isView.set(true);
                    if (authCd == GoodsAuthCd.DOWNLOAD) isDownload.set(true);
                    if (isView.get() && isDownload.get()) break;
                }

            });

            return CompanySetting.builder()
                .isViewable(isView.get())
                .isDownloadable(isDownload.get())
                .usageBeginDate(start.get())
                .usageEndDate(end.get())
                .build();
        }

    }

    @Builder
    public CompanyGoodsMapDetailRes(CompanyGoodsMapRes companyGoodsMapRes, String goodsNm, String goodsExpn, CompanySetting companySetting) {
        this.companyGoodsMapRes = companyGoodsMapRes;
        this.goodsNm = goodsNm;
        this.goodsExpn = goodsExpn;
        this.companySetting = companySetting;
    }

    public static CompanyGoodsMapDetailRes of(CustomerCompanyGoodsMapping goodsMapping) {
        return CompanyGoodsMapDetailRes.builder()
            .companyGoodsMapRes(CompanyGoodsMapRes.of(goodsMapping))
            .goodsNm(goodsMapping.getGoods().getGoodsNm())
            .goodsExpn(goodsMapping.getGoods().getGoodsExpn())
            .build();

    }

    public static CompanyGoodsMapDetailRes of(CustomerCompanyGoodsMapping goodsMapping, List<AuthManageGoodsMapping> userAuthMapping) {
        return CompanyGoodsMapDetailRes.builder()
            .companyGoodsMapRes(CompanyGoodsMapRes.of(goodsMapping))
            .goodsNm(goodsMapping.getGoods().getGoodsNm())
            .goodsExpn(goodsMapping.getGoods().getGoodsExpn())
            .companySetting(CompanySetting.of(userAuthMapping))
            .build();
    }

}
