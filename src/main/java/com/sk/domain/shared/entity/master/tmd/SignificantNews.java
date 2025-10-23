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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 중요 뉴스 Entity
 */
@Entity
@Table(
    name = "tmd_sgt_nws",
    indexes = {
        @Index(name = "tmd_sgt_news_ix_01", columnList = "nws_sig_id"),
        @Index(name = "tmd_sgt_news_ix_02", columnList = "nws_sig_id, crtr_dt)")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignificantNews extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sgt_nws_id", columnDefinition = "INT UNSIGNED")
    @Comment("중요뉴스 ID")
    private Long sgtNwsId;

    @Column(name = "nws_sig_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("뉴스중요도 ID")
    private Long nwsSigId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_sig_id")
    private NewsSignificance newsSignificance;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자")
    private String crtrDt;

    @Column(name = "impct_scre", precision = 4, scale = 2, nullable = false)
    @Comment("영향도점수")
    private BigDecimal impctScre;

    @Column(name = "drctnlt", length = 20, nullable = false)
    @Comment("방향성")
    private String drctnlt;

    @Column(name = "evnt_dtctn", length = 1000)
    @Comment("이벤트탐지 - 이벤트 판단")
    private String evntDtctn;

    @Lob
    @Column(name = "derv_rsn", columnDefinition = "TEXT")
    @Comment("도출이유")
    private String dervRsn;

    @Builder
    public SignificantNews(NewsSignificance newsSignificance, BigDecimal impctScre,
                          String drctnlt, String evntDtctn, String dervRsn) {
        this.newsSignificance = newsSignificance;
        this.impctScre = impctScre;
        this.drctnlt = drctnlt;
        this.evntDtctn = evntDtctn;
        this.dervRsn = dervRsn;
    }
}
