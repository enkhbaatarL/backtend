package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tif.EconomicCalendar;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 경제 지표 분석 관계 Entity
 */
@Entity
@Table(
    name = "tmd_ecnmcindctranlyreld",
    indexes = {
        @Index(name = "tmd_ecnmcindctranlyreld_ix01", columnList = "ecnmc_indctr_anly_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_ecnmcindctranlyreld_ux01", columnNames = {"ecnmc_indctr_anly_id", "ecnmc_cal_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_경제 지표 분석 관계")
public class EconomicIndicatorAnalysisRelationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ecnmc_indctr_anly_rel_id", columnDefinition = "INT UNSIGNED")
    @Comment("경제 지표 분석 관계 ID")
    private Long economicIndicatorAnalysisRelationshipId;

    @Column(name = "ecnmc_indctr_anly_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("경제 지표 분석 ID")
    private Long economicIndicatorAnalysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecnmc_indctr_anly_id", foreignKey = @jakarta.persistence.ForeignKey(name = "tmd_ecnmcindctranlyreld_fk01"))
    private EconomicIndicatorAnalysis economicIndicatorAnalysis;

    @Column(name = "ecnmc_cal_id", columnDefinition = "BIGINT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("경제 달력 데이터 ID")
    private Long economicCalendarId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecnmc_cal_id", foreignKey = @jakarta.persistence.ForeignKey(name = "tmd_ecnmcindctranlyreld_fk02"))
    private EconomicCalendar economicCalendar;

    @Builder
    public EconomicIndicatorAnalysisRelationship(EconomicIndicatorAnalysis economicIndicatorAnalysis,
                                                  EconomicCalendar economicCalendar) {
        this.economicIndicatorAnalysis = economicIndicatorAnalysis;
        this.economicCalendar = economicCalendar;
    }
}
