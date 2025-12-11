package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 뉴스 이벤트 Entity
 */
@Entity
@Table(
    name = "tmd_nws_evnt",
    indexes = {
        @Index(name = "tmd_nws_evnt_ix01", columnList = "crtr_dt"),
        @Index(name = "tmd_nws_evnt_ix02", columnList = "nws_smry_id"),
        @Index(name = "tmd_nws_evnt_ix03", columnList = "evnt_ctgr_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nws_evnt_ux01", columnNames = {"crtr_dt", "nm_kr"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_뉴스 이벤트")
public class NewsEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_evnt_id", columnDefinition = "INT UNSIGNED")
    @Comment("뉴스 이벤트 ID")
    private Long newsEventId;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자 (YYYYMMDD) - 뉴스 분석 기준일자")
    private String crtrDt;

    @Column(name = "evnt_ctgr_id", columnDefinition = "INT UNSIGNED", insertable = false, updatable = false)
    @Comment("이벤트 카테고리 ID")
    private Long eventCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evnt_ctgr_id")
    @Comment("이벤트 카테고리")
    private NewsEventCategory eventCategory;

    @Column(name = "nm_kr", length = 200, nullable = false)
    @Comment("이벤트 한글명")
    private String nameKr;

    @Column(name = "nm_en", length = 200, nullable = false)
    @Comment("이벤트 영문명")
    private String nameEn;

    @Column(name = "scre", precision = 4, scale = 2)
    @Comment("점수 (BE 에서 sentiment(upward/neutral/downward) 변환)")
    private BigDecimal score;

    @Column(name = "nws_smry_id", columnDefinition = "INT UNSIGNED", insertable = false, updatable = false)
    @Comment("이벤트의 대표 뉴스 요약 ID")
    private Long newsSummaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id")
    @Comment("대표 뉴스 요약")
    private NewsSummary representativeNewsSummary;

    @Builder
    public NewsEvent(String crtrDt, NewsEventCategory eventCategory, String nameKr,
                    String nameEn, BigDecimal score, NewsSummary representativeNewsSummary) {
        this.crtrDt = crtrDt;
        this.eventCategory = eventCategory;
        this.nameKr = nameKr;
        this.nameEn = nameEn;
        this.score = score;
        this.representativeNewsSummary = representativeNewsSummary;
    }
}
