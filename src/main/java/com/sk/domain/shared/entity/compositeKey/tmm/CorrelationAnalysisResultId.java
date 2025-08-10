package com.sk.domain.shared.entity.compositeKey.tmm;

import java.io.Serializable;

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
public class CorrelationAnalysisResultId implements Serializable {
    @Column(name = "mrkt_idx_anly_tgt_cd", length = 15, nullable = false)
    @Comment("시장지수분석대상코드")
    private String marketIndexAnalysisTargetCd;

    @Column(name = "anly_fulf_dt", length = 8, nullable = false)
    @Comment("분석수행일자")
    private String analysisFulfilmentDt;

    @Column(name = "mnmnc_cd", length = 50, nullable = false)
    @Comment("MNEMONIC코드")
    private String mnemonicCd;

    @Column(name = "corr_calc_cyc_cd", length = 5, nullable = false)
    @Comment("상관관계계산주기코드")
    private String correlationCalculationCycleCd;

    @Column(name = "prdc_rslt_dt", length = 8, nullable = false)
    @Comment("예측결과일자")
    private String predictionResultDt;

    @Column(name = "prdc_inpt_mnmnc_cd", length = 50, nullable = false)
    @Comment("예측입력MNEMONIC코드")
    private String predictionInputMnemonicCd;

    @Builder
    public CorrelationAnalysisResultId(String marketIndexAnalysisTargetCd, String analysisFulfilmentDt,
            String mnemonicCd, String correlationCalculationCycleCd, String predictionResultDt, String predictionInputMnemonicCd) {
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.analysisFulfilmentDt = analysisFulfilmentDt;
        this.mnemonicCd = mnemonicCd;
        this.correlationCalculationCycleCd = correlationCalculationCycleCd;
        this.predictionResultDt = predictionResultDt;
        this.predictionInputMnemonicCd = predictionInputMnemonicCd;
    }

}
