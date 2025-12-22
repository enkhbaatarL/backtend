package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 경제 지표 분석 Entity
 */
@Entity
@Table(
    name = "tmd_ecnmcindctranlym",
    indexes = {
        @Index(name = "tmd_ecnmcindctranlym_ix_01", columnList = "crtr_dt, nws_mrkt_ctgr_cd")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_ecnmcindctranlym_ux01", columnNames = {"crtr_dt", "nws_mrkt_ctgr_cd"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_경제 지표 분석")
public class EconomicIndicatorAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ecnmc_indctr_anly_id", columnDefinition = "INT UNSIGNED")
    @Comment("경제 지표 분석 ID")
    private Long economicIndicatorAnalysisId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nws_mrkt_ctgr_cd", length = 20, nullable = false)
    @Comment("뉴스 시장 카테고리 코드")
    private MarketCategoryCode nwsMrktCtgrCd;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자 (YYYYMMDD)")
    private String crtrDt;

    @Column(name = "anly_cntt", columnDefinition = "TEXT")
    @Comment("분석 내용 (줄바꿈 기호로 항목 구분)")
    private String analysisCntt;

    @Builder
    public EconomicIndicatorAnalysis(MarketCategoryCode nwsMrktCtgrCd, String crtrDt, String analysisCntt) {
        this.nwsMrktCtgrCd = nwsMrktCtgrCd;
        this.crtrDt = crtrDt;
        this.analysisCntt = analysisCntt;
    }

    /**
     * 분석 내용 업데이트
     */
    public void updateAnalysisContent(String analysisCntt) {
        this.analysisCntt = analysisCntt;
    }
}
