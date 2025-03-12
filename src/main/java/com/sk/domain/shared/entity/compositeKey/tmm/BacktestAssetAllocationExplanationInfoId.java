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
public class BacktestAssetAllocationExplanationInfoId implements Serializable {

    @Column(name = "cust_cpny_nm", nullable = false, length = 100)
    @Comment("고객회사명")
    private String customerCompanyName;

    @Column(name = "crtr_dt", nullable = false, length = 8)
    @Comment("기준일자")
    private String criteriaDate;

    @Column(name = "stg_id", nullable = false, length = 6)
    @Comment("전략ID")
    private String strategyId;

    @Column(name = "ascl_lvl_nm", nullable = false, length = 50)
    @Comment("자산군레벨명")
    private String assetClassLevelName;

    @Column(name = "aset_allc_dstg_cd", nullable = false, length = 2)
    @Comment("자산배분식별코드")
    private String assetAllocationDistinguishCode;

    @Column(name = "var_nm", nullable = false, length = 500)
    @Comment("변수명")
    private String variableName;

    @Builder
    public BacktestAssetAllocationExplanationInfoId(String customerCompanyName, String criteriaDate, String strategyId,
            String assetClassLevelName, String assetAllocationDistinguishCode,
            String variableName) {
        this.customerCompanyName = customerCompanyName;
        this.criteriaDate = criteriaDate;
        this.strategyId = strategyId;
        this.assetClassLevelName = assetClassLevelName;
        this.assetAllocationDistinguishCode = assetAllocationDistinguishCode;
        this.variableName = variableName;
    }
}