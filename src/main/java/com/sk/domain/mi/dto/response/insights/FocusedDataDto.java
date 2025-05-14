package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

public class FocusedDataDto implements FocusedDataRes {
    private int providerNo;
    private String providerNm;
    private int goodsNo;
    private String goodsNm;
    private int goodsDataNo;
    private String goodsDataNm;
    private LocalDateTime modifyDt;
    private String goodsDataCategoryCd;

    public FocusedDataDto(
            int providerNo, String providerNm,
            int goodsNo, String goodsNm,
            int goodsDataNo, String goodsDataNm,
            LocalDateTime modifyDt,
            String goodsDataCategoryCd) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataNm = goodsDataNm;
        this.modifyDt = modifyDt;
        this.goodsDataCategoryCd = goodsDataCategoryCd;
    }

    @Override
    public String getGoodsDataCategoryCd() {
        return goodsDataCategoryCd;
    }

    // setter 추가
    public void setGoodsDataCategoryCd(String goodsDataCategoryCd) {
        this.goodsDataCategoryCd = goodsDataCategoryCd;
    }

    @Override
    public int getProviderNo() {
        return providerNo;
    }

    @Override
    public String getProviderNm() {
        return providerNm;
    }

    @Override
    public int getGoodsNo() {
        return goodsNo;
    }

    @Override
    public String getGoodsNm() {
        return goodsNm;
    }

    @Override
    public int getGoodsDataNo() {
        return goodsDataNo;
    }

    @Override
    public String getGoodsDataNm() {
        return goodsDataNm;
    }

    @Override
    public LocalDateTime getModifyDt() {
        return modifyDt;
    }
}
