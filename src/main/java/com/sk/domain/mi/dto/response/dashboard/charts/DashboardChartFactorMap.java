package com.sk.domain.mi.dto.response.dashboard.charts;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DashboardChartFactorMap {
    /**
     * 시장지표분석대상코드
     */
    String marketIndexAnalysisTargetCd;

    /**
     * 예측수행일자
     */
    String predictionFulfillDt;

    /**
     * 예측입력명목상세코드
     */
    String predictionInputMnemonicDetailCd;

    /**
     * 예측입력명목코드
     */
    String predictionInputMnemonicCd;

    /**
     * 설명력명목상세코드
     */
    String explanatoryLevelDetailNm;

    /**
     * 설명력점수
     */
    BigDecimal explanatoryScore;

    /**
     * 설명력순위
     */
    Integer explanatoryRnk;

    /**
     * MNEMONIC코드
     */
    String mnemonicCd;

    /**
     * 설명력방향기호명
     */
    String explanatoryDirectionSymbolNm;

    /**
     * 설명력순위
     */
    Integer dirRank;
}
