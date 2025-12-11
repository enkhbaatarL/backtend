package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 뉴스 이벤트 카테고리 Entity
 * CAMEO(Conflict and Mediation Event Observations) 기반 이벤트 분류
 */
@Entity
@Table(
    name = "tmd_nws_evnt_ctgr",
    indexes = {
        @Index(name = "tmd_nws_evnt_ctgr_ix01", columnList = "root_code"),
        @Index(name = "tmd_nws_evnt_ctgr_ix02", columnList = "quad_class")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nws_evnt_ctgr_ux01", columnNames = {"root_code", "quad_class"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_뉴스 이벤트 카테고리")
public class NewsEventCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evnt_ctgr_id", columnDefinition = "INT UNSIGNED")
    @Comment("이벤트 카테고리 ID")
    private Long eventCategoryId;

    @Column(name = "root_code", columnDefinition = "SMALLINT UNSIGNED", nullable = false)
    @Comment("CAMEO 1단계 Root Code (01부터 20까지) + 추가예정")
    private Integer rootCode;

    @Column(name = "quad_class", columnDefinition = "SMALLINT UNSIGNED", nullable = false)
    @Comment("범주 분류 코드 (1: 구두 협력, 4: 실제 갈등)")
    private Integer quadClass;

    @Column(name = "description_kr", length = 100, nullable = false)
    @Comment("한글 설명 (예: 의견 표명, 무력 사용)")
    private String descriptionKr;

    @Column(name = "description_en", length = 100, nullable = false)
    @Comment("영문 설명")
    private String descriptionEn;

    @Column(name = "nature", length = 20, nullable = false)
    @Comment("성격 구분 (예: 협력 또는 갈등)")
    private String nature;

    @Column(name = "goldstein_max", precision = 5, scale = 2)
    @Comment("최대 Goldstein 점수")
    private BigDecimal goldsteinMax;

    @Column(name = "goldstein_min", precision = 5, scale = 2)
    @Comment("최소 Goldstein 점수")
    private BigDecimal goldsteinMin;

    @Builder
    public NewsEventCategory(Integer rootCode, Integer quadClass, String descriptionKr,
                            String descriptionEn, String nature, BigDecimal goldsteinMax,
                            BigDecimal goldsteinMin) {
        this.rootCode = rootCode;
        this.quadClass = quadClass;
        this.descriptionKr = descriptionKr;
        this.descriptionEn = descriptionEn;
        this.nature = nature;
        this.goldsteinMax = goldsteinMax;
        this.goldsteinMin = goldsteinMin;
    }
}
