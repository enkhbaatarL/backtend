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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 시장 리캡 하이라이트 이벤트 관계 Entity
 */
@Entity
@Table(
    name = "tmd_mrkt_rcap_hl_evnt_rel",
    indexes = {
        @Index(name = "tmd_mrkt_rcap_hl_evnt_rel_ix01", columnList = "mrkt_rcap_hl_evnt_id"),
        @Index(name = "tmd_mrkt_rcap_hl_evnt_rel_ix02", columnList = "nws_evnt_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mrkt_rcap_hl_evnt_rel_ux01", columnNames = {"mrkt_rcap_hl_evnt_id", "nws_evnt_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_마켓 리캡 투데이 하이라이트 이벤트 관계")
public class MarketRecapHighlightEventRelationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_hl_evnt_rel_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 하이라이트 이벤트 관계 ID")
    private Long mrktRcapHlEvntRelId;

    @Column(name = "mrkt_rcap_hl_evnt_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("시장 리캡 하이라이트 이벤트 ID")
    private Long mrktRcapHlEvntId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrkt_rcap_hl_evnt_id", nullable = false)
    @Comment("시장 리캡 하이라이트 이벤트")
    private MarketRecapHighlightEvent marketRecapHighlightEvent;

    @Column(name = "nws_evnt_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("뉴스 이벤트 ID")
    private Long newsEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_evnt_id", nullable = false)
    @Comment("뉴스 이벤트")
    private NewsEvent newsEvent;

    @Builder
    public MarketRecapHighlightEventRelationship(MarketRecapHighlightEvent marketRecapHighlightEvent,
                                                 NewsEvent newsEvent) {
        this.marketRecapHighlightEvent = marketRecapHighlightEvent;
        this.newsEvent = newsEvent;
    }
}
