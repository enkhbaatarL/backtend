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
public class AssetAllocationInfoId implements Serializable {

    @Column(name = "cust_cpny_nm", nullable = false, length = 100)
    @Comment("고객회사명")
    private String customerCompanyName;

    @Column(name = "crtr_dt", nullable = false, length = 8)
    @Comment("기준일자")
    private String criteriaDate;

    @Column(name = "stg_id", nullable = false, length = 6)
    @Comment("전략ID")
    private String strategyId;

    @Column(name = "mdl_id", nullable = false, length = 50)
    @Comment("모델ID")
    private String modelId;

    @Column(name = "rglr_rbln_cyc", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("정기리밸런싱주기")
    @ColumnDefault("0")
    private Integer regularRebalancingCycle;

    @Column(name = "aset_grp_nm", nullable = false, length = 50)
    @Comment("자산그룹명")
    private String assetGroupName;

    @Builder
    public AssetAllocationInfoId(String customerCompanyName, String criteriaDate, String strategyId,
            String modelId, Integer regularRebalancingCycle, String assetGroupName) {
        this.customerCompanyName = customerCompanyName;
        this.criteriaDate = criteriaDate;
        this.strategyId = strategyId;
        this.modelId = modelId;
        this.regularRebalancingCycle = regularRebalancingCycle;
        this.assetGroupName = assetGroupName;
    }
}