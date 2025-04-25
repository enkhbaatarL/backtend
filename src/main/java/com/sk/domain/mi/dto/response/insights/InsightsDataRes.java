package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

public record InsightsDataRes(
        int providerNo,
        String providerNm,
        int goodsNo,
        int goodsDataNo,
        String goodsDataNm,
        String goodsDataExpn,
        LocalDateTime modifyDt) {

}
