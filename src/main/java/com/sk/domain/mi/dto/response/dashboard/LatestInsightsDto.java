package com.sk.domain.mi.dto.response.dashboard;

import java.time.LocalDateTime;

// @formatter:off
public interface LatestInsightsDto {
        int getProviderNo();
        String getProviderNm();
        int getGoodsNo();
        String getGoodsNm();
        int getGoodsDataNo();
        String getGoodsDataNm();
        LocalDateTime getModifyDt();
}
// @formatter:on