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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 시장 리캡 체크포인트 Entity
 */
@Entity
@Table(
    name = "tmd_mrkt_rcap_ckpt",
    indexes = {
        @Index(name = "tmd_mrkt_rcap_ckpt_itm_ix01", columnList = "mrkt_rcap_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_마켓 리캡 체크포인트")
public class MarketRecapCheckpoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_ckpt_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 체크포인트 ID")
    private Long mrktRcapCkptId;

    @Column(name = "mrkt_rcap_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("시장 리캡 ID")
    private Long mrktRcapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrkt_rcap_id", nullable = false)
    @Comment("시장 리캡")
    private MarketRecap marketRecap;

    @Column(name = "titl", length = 500)
    @Comment("제목 (focused, detailed 모드시 표시)")
    private String title;

    @Column(name = "cntt", length = 1000)
    @Comment("내용 (detailed 모드시 표시)")
    private String content;

    @Builder
    public MarketRecapCheckpoint(MarketRecap marketRecap, String title, String content) {
        this.marketRecap = marketRecap;
        this.title = title;
        this.content = content;
    }

    /**
     * 체크포인트 내용 업데이트
     */
    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
