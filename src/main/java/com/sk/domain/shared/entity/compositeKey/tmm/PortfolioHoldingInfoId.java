package com.sk.domain.shared.entity.compositeKey.tmm;

import java.io.Serializable;

import org.hibernate.annotations.ColumnDefault;
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
public class PortfolioHoldingInfoId implements Serializable {

    @Column(name = "cpny_sbcm_spt_cd", nullable = false, length = 100)
    @Comment("회사분과구분코드")
    private String companySubcommitteeSeparateCd;

    @Column(name = "crtr_dt", nullable = false, length = 8)
    @Comment("기준일자")
    private String criteriaDate;

    @Column(name = "stg_id", nullable = false, length = 6)
    @Comment("전략ID")
    private String strategyId;

    @Column(name = "rglr_rbln_cyc", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("정기리밸런싱주기")
    @ColumnDefault("0")
    private Integer regularRebalancingCycle;

    @Column(name = "ric_cd", nullable = false, length = 15)
    @Comment("RIC코드")
    private String ricCode;

    @Builder
    public PortfolioHoldingInfoId(String companySubcommitteeSeparateCd, String criteriaDate, String strategyId,
            Integer regularRebalancingCycle, String ricCode) {
        this.companySubcommitteeSeparateCd = companySubcommitteeSeparateCd;
        this.criteriaDate = criteriaDate;
        this.strategyId = strategyId;
        this.regularRebalancingCycle = regularRebalancingCycle;
        this.ricCode = ricCode;
    }
}