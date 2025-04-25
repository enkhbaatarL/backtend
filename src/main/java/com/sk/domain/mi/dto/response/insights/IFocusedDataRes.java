package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

public interface IFocusedDataRes {
    int getProviderNo();

    String getProviderNm();

    int getGoodsNo();

    String getGoodsNm();

    int getGoodsDataNo();

    String getGoodsDataNm();

    LocalDateTime getModifyDt();

    String getGoodsDataCategoryCd();
}
