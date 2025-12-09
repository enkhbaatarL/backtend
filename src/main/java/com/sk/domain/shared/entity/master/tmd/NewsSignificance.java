package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 뉴스 중요도 Entity
 */
@Entity
@Table(
    name = "tmd_nws_sig",
    indexes = {
        @Index(name = "tmd_nws_sig_ix01", columnList = "nws_smry_id"),
        @Index(name = "tmd_nws_sig_ix02", columnList = "nws_mrkt_ctgr_cd"),
        @Index(name = "tmd_nws_sig_ix03", columnList = "nws_mrkt_ctgr_cd, crtr_dt")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nws_sig_ux01", columnNames = {"nws_smry_id", "nws_mrkt_ctgr_cd"})
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsSignificance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_sig_id", columnDefinition = "INT UNSIGNED")
    @Comment("뉴스중요도 ID")
    private Long nwsSigId;

    @Column(name = "nws_smry_id", nullable = false, columnDefinition = "int unsigned", insertable = false, updatable = false)
    @Comment("뉴스 요약 ID")
    private Long newsSummaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", referencedColumnName = "nws_smry_id", nullable = false)
    @Comment("뉴스 요약")
    private NewsSummary newsSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "nws_mrkt_ctgr_cd", length = 20, nullable = false)
    @Comment("뉴스시장카테고리코드 - 시장 분류 (Market Category) 원유, 천연가스, 반도체, EV배터리 등으로 분류")
    private MarketCategoryCode nwsMrktCtgrCd;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자")
    private String crtrDt;

    @Column(name = "scre", precision = 4, scale = 2)
    @Comment("점수")
    private BigDecimal score;

    @Builder
    public NewsSignificance(NewsSummary newsSummary, MarketCategoryCode nwsMrktCtgrCd,
                           String crtrDt, BigDecimal score) {
        this.newsSummary = newsSummary;
        this.nwsMrktCtgrCd = nwsMrktCtgrCd;
        this.crtrDt = crtrDt;
        this.score = score;
    }

}
