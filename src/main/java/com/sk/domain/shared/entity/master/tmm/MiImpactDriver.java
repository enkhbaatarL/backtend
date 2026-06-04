package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.ImpactGrade;
import com.sk.domain.shared.converter.ImpactGradeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Comment("Driver Tag 마스터")
@Table(name = "tmm_mi_impact_driver_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiImpactDriver extends BaseEntity {

    @Id
    @Column(name = "driver_id", length = 64, nullable = false)
    @Comment("Driver Tag ID PK")
    private String driverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impact_result_id", nullable = false)
    @Comment("Impact Result ID FK")
    private MiImpactResult impactResult;

    @Column(name = "lv1_id", length = 50, nullable = false)
    @Comment("Driver Tree 1단계 코드")
    private String lv1Id;

    @Column(name = "lv1_label", length = 100)
    @Comment("Driver Tree 1단계 명칭")
    private String lv1Label;

    @Column(name = "lv2_id", length = 50, nullable = false)
    @Comment("Driver Tree 2단계 코드")
    private String lv2Id;

    @Column(name = "lv2_label", length = 100)
    @Comment("Driver Tree 2단계 명칭")
    private String lv2Label;

    @Column(name = "lv3_id", length = 50, nullable = false)
    @Comment("Driver Tree 3단계 코드")
    private String lv3Id;

    @Column(name = "lv3_label", length = 100)
    @Comment("Driver Tree 3단계 명칭")
    private String lv3Label;

    @Column(name = "direction_hint", length = 50)
    @Comment("방향 힌트 bullish_if_up/bearish_if_up/contextual")
    private String directionHint;

    @Column(name = "match_score", precision = 5, scale = 4)
    @Comment("Event-Driver 매핑 신뢰도 점수 0~1")
    private BigDecimal matchScore;

    @Column(name = "matched_keywords", columnDefinition = "json")
    @Comment("매칭된 키워드 목록")
    private String matchedKeywords;

    @Column(name = "driver_reason", columnDefinition = "text")
    @Comment("Driver 영향 발생 사유")
    private String driverReason;

    @Convert(converter = ImpactGradeConverter.class)
    @Column(name = "impact_short", nullable = false, columnDefinition = "enum('상승_상','상승_중','상승_하','영향없음','하락_하','하락_중','하락_상')")
    @Comment("driver의 단기 영향 {방향_강도}")
    private ImpactGrade impactShort;

    @Convert(converter = ImpactGradeConverter.class)
    @Column(name = "impact_mid", columnDefinition = "enum('상승_상','상승_중','상승_하','영향없음','하락_하','하락_중','하락_상')")
    @Comment("driver의 중기 영향")
    private ImpactGrade impactMid;

    @Convert(converter = ImpactGradeConverter.class)
    @Column(name = "impact_long", columnDefinition = "enum('상승_상','상승_중','상승_하','영향없음','하락_하','하락_중','하락_상')")
    @Comment("driver의 장기 영향")
    private ImpactGrade impactLong;

    @Builder
    public MiImpactDriver(String driverId, MiImpactResult impactResult,
                          String lv1Id, String lv1Label, String lv2Id, String lv2Label,
                          String lv3Id, String lv3Label, String directionHint, BigDecimal matchScore,
                          String matchedKeywords, String driverReason,
                          ImpactGrade impactShort, ImpactGrade impactMid, ImpactGrade impactLong) {
        this.driverId = driverId;
        this.impactResult = impactResult;
        this.lv1Id = lv1Id;
        this.lv1Label = lv1Label;
        this.lv2Id = lv2Id;
        this.lv2Label = lv2Label;
        this.lv3Id = lv3Id;
        this.lv3Label = lv3Label;
        this.directionHint = directionHint;
        this.matchScore = matchScore;
        this.matchedKeywords = matchedKeywords;
        this.driverReason = driverReason;
        this.impactShort = impactShort;
        this.impactMid = impactMid;
        this.impactLong = impactLong;
    }

}
