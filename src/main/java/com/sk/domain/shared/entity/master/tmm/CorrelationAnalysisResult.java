package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmm.CorrelationAnalysisResultId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMM_상관관계분석결과상세")
@Table(name = "tmm_corranlyrsltd")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorrelationAnalysisResult extends BaseEntity {

    @EmbeddedId
    private CorrelationAnalysisResultId id;

    @Column(name = "instm_nm", length = 255, nullable = false)
    @Comment("INSTRUMENT명")
    private String instrumentNm;

    @Column(name = "corr_calc_val", nullable = false, precision = 16, scale = 4)
    @Comment("상관관계계산값")
    private BigDecimal correlationCalculationVal;

    @Builder
    public CorrelationAnalysisResult(CorrelationAnalysisResultId id, String instrumentNm,
            BigDecimal correlationCalculationVal) {
        this.id = id;
        this.instrumentNm = instrumentNm;
        this.correlationCalculationVal = correlationCalculationVal;
    }
}
