package com.sk.domain.mi.dto.response.dashboard.summary;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DashboardChartSummaryMap {
    /**
     * 시장지수분석대상코드
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
     * 예측결과일자
     */
    String predictionResultDt;

    /**
     * 설명력제목내용
     */
    String explanatoryTitleContent;
    
    /**
     * 설명력요약내용
     */
    String explanatorySummaryContent;

    /**
     * 설명력내용파일경로명
     */
    String explanatoryFilePath;
}
