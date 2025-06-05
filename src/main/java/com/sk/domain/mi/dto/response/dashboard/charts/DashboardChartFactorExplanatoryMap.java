package com.sk.domain.mi.dto.response.dashboard.charts;

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
     * 예측입력MNEMONIC코드
     */
    String predictionInputMnemonicCd;

    /**
     * 설명력명목명
     */
    String explanatoryLevelNm;

    /**
     * 설명력상세내용
     */
    String explanatoryDetailCntt;

    /**
     * 설명력레벨상세명
     */
    String explanatoryLevelDetailNm;
}
