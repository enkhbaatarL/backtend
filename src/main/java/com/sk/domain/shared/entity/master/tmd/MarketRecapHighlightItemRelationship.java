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
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 시장 리캡 하이라이트 아이템 관계 Entity
 */
@Entity
@Table(
    name = "tmd_mrkt_rcap_hl_itm_rel",
    indexes = {
        @Index(name = "tmd_mrkt_rcap_hl_itm_rel_ix_01", columnList = "mrkt_rcap_hl_itm_id"),
        @Index(name = "tmd_mrkt_rcap_hl_itm_rel_ix_02", columnList = "nws_smry_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mrkt_rcap_hl_itm_rel_ux01", columnNames = {"mrkt_rcap_hl_itm_id", "nws_smry_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketRecapHighlightItemRelationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_hl_itm_rel_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 하이라이트 아이템 관계 ID")
    private Long mrktRcapHlItmRelId;

    @Column(name = "mrkt_rcap_hl_itm_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("시장 리캡 하이라이트 아이템 ID")
    private Long mrktRcapHlItmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrkt_rcap_hl_itm_id", foreignKey = @jakarta.persistence.ForeignKey(name = "mrkt_rcap_hl_itm_rel_fk01"))
    private MarketRecapHighlightItem marketRecapHighlightItem;

    @Column(name = "nws_smry_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("뉴스 요약 ID")
    private Long nwsSmryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", foreignKey = @jakarta.persistence.ForeignKey(name = "mrkt_rcap_hl_itm_rel_fk02"))
    private NewsSummary newsSummary;

    @Builder
    public MarketRecapHighlightItemRelationship(MarketRecapHighlightItem marketRecapHighlightItem, NewsSummary newsSummary) {
        this.marketRecapHighlightItem = marketRecapHighlightItem;
        this.newsSummary = newsSummary;
    }
}
