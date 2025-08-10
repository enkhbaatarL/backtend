package com.sk.domain.shared.entity.compositeKey.tmm;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class BizTargetPredictionExplanatoryInfoId {

    @Column(name = "mrkt_idx_anly_tgt_cd", nullable = false, length = 15)
    @Comment("시장지수분석대상코드")
    private String marketIndexAnalysisTargetCd;

    @Column(name = "prdc_fulf_dt", nullable = false, length = 8)
    @Comment("예측수행일자")
    private String predictionFulfillDt;

    @Column(name = "mnmnc_cd", nullable = false, length = 50)
    @Comment("MNEMONIC코드")
    private String mnemonicCd;

    @Column(name = "expp_lvl_nm", nullable = false, length = 100)
    @Comment("설명력레벨명")
    private String explanatoryLevelNm;

    @Column(name = "expp_lvl_dtl_nm", nullable = false, length = 500)
    @Comment("설명력레벨상세명")
    private String explanatoryLevelDetailNm;

    @Column(name = "prdc_inpt_mnmnc_cd", nullable = false, length = 50)
    @Comment("예측입력MNEMONIC코드")
    private String predictionInputMnemonicCd;

    @Builder
    public BizTargetPredictionExplanatoryInfoId(String marketIndexAnalysisTargetCd, String predictionFulfillDt, String mnemonicCd,
            String explanatoryLevelNm, String explanatoryLevelDetailNm, String predictionInputMnemonicCd) {
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.predictionFulfillDt = predictionFulfillDt;
        this.mnemonicCd = mnemonicCd;
        this.explanatoryLevelNm = explanatoryLevelNm;
        this.explanatoryLevelDetailNm = explanatoryLevelDetailNm;
        this.predictionInputMnemonicCd = predictionInputMnemonicCd;
    }

    public static BizTargetPredictionExplanatoryInfoId of(String marketIndexAnalysisTargetCd, String predictionFulfillDt, String mnemonicCd,
            String explanatoryLevelNm, String explanatoryLevelDetailNm, String predictionInputMnemonicCd) {
        return BizTargetPredictionExplanatoryInfoId.builder()
                .marketIndexAnalysisTargetCd(marketIndexAnalysisTargetCd)
                .predictionFulfillDt(predictionFulfillDt)
                .mnemonicCd(mnemonicCd)
                .explanatoryLevelNm(explanatoryLevelNm)
                .explanatoryLevelDetailNm(explanatoryLevelDetailNm)
                .predictionInputMnemonicCd(predictionInputMnemonicCd)
                .build();
    }

}
