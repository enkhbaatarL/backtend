package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class FocusedDataRes implements FocusedDataDto {
    private final int providerNo;
    private final String providerNm;
    private final int goodsNo;
    private final String goodsNm;
    private final int goodsDataNo;
    private final String goodsDataNm;
    private final LocalDateTime modifyDt;
    private final String modifyDtStr;
    private String goodsDataCategoryCd;
    private boolean isViewable;

    public FocusedDataRes(
            int providerNo, String providerNm,
            int goodsNo, String goodsNm,
            int goodsDataNo, String goodsDataNm,
            String modifyDtStr, LocalDateTime modifyDt,
            String goodsDataCategoryCd,
            boolean isViewable) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataNm = goodsDataNm;
        this.modifyDtStr = modifyDtStr;
        this.modifyDt = modifyDt;
        this.goodsDataCategoryCd = goodsDataCategoryCd;
        this.isViewable = isViewable;
    }

    // setter 추가
    public void setGoodsDataCategoryCd(String goodsDataCategoryCd) {
        this.goodsDataCategoryCd = goodsDataCategoryCd;
    }

    public void setIsViewable(boolean isViewable) {
        this.isViewable = isViewable;
    }

}
