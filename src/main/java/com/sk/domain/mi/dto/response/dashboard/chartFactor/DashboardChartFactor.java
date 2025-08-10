package com.sk.domain.mi.dto.response.dashboard.chartFactor;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartFactor(
        @Schema(description = "시장지수분석대상코드") String marketIndexAnalysisTargetCd,

        @Schema(description = "예측수행일자") String predictionFulfillDt,

        @Schema(description = "예측입력MNEMONIC상세코드_설명력변수데이터조회KEY") String predictionInputMnemonicDetailCd,

        @Schema(description = "예측입력MNEMONIC코드_화면출력설명력변수약어명") String predictionInputMnemonicCd,

        @Schema(description = "설명력레벨상세명_설명력변수") String explanatoryLevelDetailNm,

        @Schema(description = "설명력점수") BigDecimal explanatoryScore,

        @Schema(description = "설명력순위(실제)") Integer explanatoryRnk,

        @Schema(description = "MNEMONIC코드") String mnemonicCd,

        @Schema(description = "설명력방향기호명") String explanatoryDirectionSymbolNm,

        @Schema(description = "설명력순위(모델)") Integer dirRank) {

    @Builder
    public DashboardChartFactor(String marketIndexAnalysisTargetCd, String predictionFulfillDt, String predictionInputMnemonicDetailCd,
            String predictionInputMnemonicCd, String explanatoryLevelDetailNm, BigDecimal explanatoryScore, Integer explanatoryRnk,
            String mnemonicCd, String explanatoryDirectionSymbolNm, Integer dirRank) {
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.predictionFulfillDt = predictionFulfillDt;
        this.predictionInputMnemonicDetailCd = predictionInputMnemonicDetailCd;
        this.predictionInputMnemonicCd = predictionInputMnemonicCd;
        this.explanatoryLevelDetailNm = explanatoryLevelDetailNm;
        this.explanatoryScore = explanatoryScore;
        this.explanatoryRnk = explanatoryRnk;
        this.mnemonicCd = mnemonicCd;
        this.explanatoryDirectionSymbolNm = explanatoryDirectionSymbolNm;
        this.dirRank = dirRank;
    }

    public static DashboardChartFactor of(DashboardChartFactorMap map) {
        return DashboardChartFactor.builder()
                .marketIndexAnalysisTargetCd(map.getMarketIndexAnalysisTargetCd())
                .predictionFulfillDt(map.getPredictionFulfillDt())
                .predictionInputMnemonicDetailCd(map.getPredictionInputMnemonicDetailCd())
                .predictionInputMnemonicCd(map.getPredictionInputMnemonicCd())
                .explanatoryLevelDetailNm(map.getExplanatoryLevelDetailNm())
                .explanatoryScore(map.getExplanatoryScore())
                .explanatoryRnk(map.getExplanatoryRnk())
                .mnemonicCd(map.getMnemonicCd())
                .explanatoryDirectionSymbolNm(map.getExplanatoryDirectionSymbolNm())
                .dirRank(map.getDirRank())
                .build();
    }
}
