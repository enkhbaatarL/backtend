package com.sk.domain.shared.entity.master.tmm;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("MI Event Impact 마스터")
@Table(name = "tmm_mi_impact_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiImpact extends BaseEntity {

    @Id
    @Column(name = "impact_id", length = 40, nullable = false)
    @Comment("Impact ID PK (impact_ev_[a-z0-9_]+)")
    private String impactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mi_event_id", nullable = false)
    @Comment("MI Event ID FK")
    private MiEvent miEvent;

    @Column(name = "base_case_id", length = 60)
    @Comment("MI Event Base Case ID FK")
    private String baseCaseId;

    @Column(name = "base_case_skip", nullable = false)
    @Comment("Base Case 미명시로 단기만 산출한 경우 true")
    private Boolean baseCaseSkip;

    @Column(name = "impact_summary", columnDefinition = "text")
    @Comment("JKM/TTF/HH 영향도 분석 결과 종합 요약 텍스트")
    private String impactSummary;

    @Builder
    public MiImpact(String impactId, MiEvent miEvent, String baseCaseId,
                    Boolean baseCaseSkip, String impactSummary) {
        this.impactId = impactId;
        this.miEvent = miEvent;
        this.baseCaseId = baseCaseId;
        this.baseCaseSkip = baseCaseSkip;
        this.impactSummary = impactSummary;
    }

}
