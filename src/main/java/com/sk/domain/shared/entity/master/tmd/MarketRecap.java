package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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
        @Index(name = "tmd_mrkt_rcap_ix01", columnList = "crtr_dt"),
        @Index(name = "tmd_mrkt_rcap_ix02", columnList = "nws_mrkt_ctgr_cd")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mrkt_rcap_ux01", columnNames = {"crtr_dt", "nws_mrkt_ctgr_cd"})
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_마켓 리캡")
public class MarketRecap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 ID")
    private Long mrktRcapId;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자 (YYYYMMDD)")
    private String crtrDt;

    @Column(name = "nws_mrkt_ctgr_cd", length = 20, nullable = false)
    @Comment("뉴스 시장 카테고리 코드")
    private String nwsMrktCtgrCd;

    @Column(name = "hl_evnt_titl", length = 500)
    @Comment("하이라이트 이벤트 제목 (narrative 모드시 표시)")
    private String hlEvntTitl;

    @Lob
    @Column(name = "hl_evnt_cntt", columnDefinition = "TEXT")
    @Comment("하이라이트 이벤트 내용 (narrative 모드시 표시)")
    private String hlEvntCntt;

    @Column(name = "ckpt_titl", length = 500)
    @Comment("체크포인트 제목 (narrative 모드시 표시)")
    private String ckptTitl;

    @Lob
    @Column(name = "ckpt_cntt", columnDefinition = "TEXT")
    @Comment("체크포인트 내용 (narrative 모드시 표시)")
    private String ckptCntt;

    @Builder
    public MarketRecap(String crtrDt, String nwsMrktCtgrCd, String hlEvntTitl,
        String hlEvntCntt, String ckptTitl, String ckptCntt) {
        this.crtrDt = crtrDt;
        this.nwsMrktCtgrCd = nwsMrktCtgrCd;
        this.hlEvntTitl = hlEvntTitl;
        this.hlEvntCntt = hlEvntCntt;
        this.ckptTitl = ckptTitl;
        this.ckptCntt = ckptCntt;
    }

    /**
     * 하이라이트 이벤트 업데이트
     */
    public void updateHighlightEvent(String title, String content) {
        this.hlEvntTitl = title;
        this.hlEvntCntt = content;
    }

    /**
     * 체크포인트 업데이트
     */
    public void updateCheckpoint(String title, String content) {
        this.ckptTitl = title;
        this.ckptCntt = content;
    }
}
