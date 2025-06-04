package com.sk.domain.mi.dto.response.dashboard.charts;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DashboardChartFactorExplanatoryMap {
    /**
     * 시장지표분석대상코드
     */
    String marketIndexAnalysisTargetCd;

    /**
     * 예측수행일자
     */
    String predictionFulfillDt;

    /**
     * MNEMONIC코드
     */
    String mnemonicCd;

    /**
     * 설명력명목명
     */
    String explanatoryLevelNm;

    /**
     * 설명력상세내용
     */
    String explanatoryDetailCntt;
}
