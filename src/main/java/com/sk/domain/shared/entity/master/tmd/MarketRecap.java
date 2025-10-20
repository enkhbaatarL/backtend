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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 마켓 리캡 Entity
 */
@Entity
@Table(
    name = "tmd_mrkt_rcap",
    indexes = {
        @Index(name = "tmd_mrkt_rcap_ix_01", columnList = "crtr_dt, nws_mrkt_ctgr_cd")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mrkt_rcap_ux01", columnNames = {"crtr_dt", "nws_mrkt_ctgr_cd"})
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketRecap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 ID")
    private Long mrktRcapId;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자")
    private String crtrDt;

    @Column(name = "nws_mrkt_ctgr_cd", length = 20, nullable = false)
    @Comment("뉴스 시장 카테고리 코드")
    private String nwsMrktCtgrCd;

    @Column(name = "upw_sgnl_nws_cnt", columnDefinition = "SMALLINT UNSIGNED", nullable = false)
    @Comment("상승 신호 뉴스 수")
    private Integer upwSgnlNwsCnt;

    @Column(name = "ntrl_sgnl_nws_cnt", columnDefinition = "SMALLINT UNSIGNED", nullable = false)
    @Comment("중립 신호 뉴스 수")
    private Integer ntrlSgnlNwsCnt;

    @Column(name = "dwn_sgnl_nws_cnt", columnDefinition = "SMALLINT UNSIGNED", nullable = false)
    @Comment("하락 신호 뉴스 수")
    private Integer dwnSgnlNwsCnt;

    @Column(name = "hi_titl", length = 200)
    @Comment("하이라이트 제목")
    private String hiTitl;

    @Column(name = "hi_cntt", length = 500)
    @Comment("하이라이트 내용")
    private String hiCntt;

    @Column(name = "ckpt_titl", length = 200)
    @Comment("체크 포인트 제목")
    private String ckptTitl;

    @Column(name = "ckpt_cntt", length = 500)
    @Comment("체크 포인트 내용")
    private String ckptCntt;

    @Builder
    public MarketRecap(String crtrDt, String nwsMrktCtgrCd, Integer upwSgnlNwsCnt,
        Integer ntrlSgnlNwsCnt, Integer dwnSgnlNwsCnt, String hiTitl,
        String hiCntt, String ckptTitl, String ckptCntt) {
        this.crtrDt = crtrDt;
        this.nwsMrktCtgrCd = nwsMrktCtgrCd;
        this.upwSgnlNwsCnt = upwSgnlNwsCnt;
        this.ntrlSgnlNwsCnt = ntrlSgnlNwsCnt;
        this.dwnSgnlNwsCnt = dwnSgnlNwsCnt;
        this.hiTitl = hiTitl;
        this.hiCntt = hiCntt;
        this.ckptTitl = ckptTitl;
        this.ckptCntt = ckptCntt;
    }
}
