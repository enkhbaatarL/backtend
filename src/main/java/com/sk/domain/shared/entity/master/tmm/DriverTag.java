package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
@Comment("TMM_DriverTag")
@Table(name = "tmm_drivertagm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverrides({
        @AttributeOverride(name = "createUserId", column = @Column(name = "crt_user_id", length = 50, nullable = false)),
        @AttributeOverride(name = "modifyUserId", column = @Column(name = "mod_user_id", length = 50, nullable = false))
})
public class DriverTag extends BaseEntity {

    @Id
    @Column(name = "driver_id", length = 64, nullable = false)
    @Comment("Driver Tag ID")
    private String driverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impact_result_id", nullable = false)
    @Comment("Impact Result ID FK")
    private ImpactResult impactResult;

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

    @Column(name = "direction", length = 20)
    @Comment("실제 영향 ��향 bullish/bearish/negligible")
    private String direction;

    @Column(name = "match_score", precision = 5, scale = 4)
    @Comment("Event-Driver 매핑 신뢰도 점수 0~1")
    private BigDecimal matchScore;

    @Column(name = "driver_magnitude", length = 20)
    @Comment("Driver 영향 강도 low/medium/high/extreme")
    private String driverMagnitude;

    @Column(name = "driver_reason", columnDefinition = "text")
    @Comment("Driver 영향 발생 사���")
    private String driverReason;

    @Column(name = "matched_keywords", columnDefinition = "json")
    @Comment("매칭된 키워드 목록")
    private String matchedKeywords;

    @Builder
    public DriverTag(String driverId, ImpactResult impactResult,
            String lv1Id, String lv1Label, String lv2Id, String lv2Label,
            String lv3Id, String lv3Label, String directionHint, String direction,
            BigDecimal matchScore, String driverMagnitude, String driverReason,
            String matchedKeywords) {
        this.driverId = driverId;
        this.impactResult = impactResult;
        this.lv1Id = lv1Id;
        this.lv1Label = lv1Label;
        this.lv2Id = lv2Id;
        this.lv2Label = lv2Label;
        this.lv3Id = lv3Id;
        this.lv3Label = lv3Label;
        this.directionHint = directionHint;
        this.direction = direction;
        this.matchScore = matchScore;
        this.driverMagnitude = driverMagnitude;
        this.driverReason = driverReason;
        this.matchedKeywords = matchedKeywords;
    }

}
