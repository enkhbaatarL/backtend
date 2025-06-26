package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class FocusedDataRes implements FocusedDataDto {
    private int providerNo;
    private String providerNm;
    private int goodsNo;
    private String goodsNm;
    private int goodsDataNo;
    private String goodsDataNm;
    private LocalDateTime modifyDt;
    private String goodsDataCategoryCd;
    private boolean isViewable;

    public FocusedDataRes(
            int providerNo, String providerNm,
            int goodsNo, String goodsNm,
            int goodsDataNo, String goodsDataNm,
            LocalDateTime modifyDt,
            String goodsDataCategoryCd,
            boolean isViewable) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataNm = goodsDataNm;
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
