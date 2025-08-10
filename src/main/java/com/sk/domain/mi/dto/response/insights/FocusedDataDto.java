package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

// GROUP_CONCAT 관련 처리로 인해 interface 로 작성
public interface FocusedDataDto {
    int getProviderNo();

    String getProviderNm();

    int getGoodsNo();

    String getGoodsNm();

    int getGoodsDataNo();

    String getGoodsDataNm();

    LocalDateTime getModifyDt();
    String getModifyDtStr();

    String getGoodsDataCategoryCd();
}
