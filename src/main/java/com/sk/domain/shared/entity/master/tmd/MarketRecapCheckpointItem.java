package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 시장 리캡 체크포인트 아이템 Entity
 */
@Entity
@Table(name = "tmd_mrkt_rcap_ckpt_itm")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketRecapCheckpointItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_ckpt_itm_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 체크포인트 아이템 ID")
    private Long mrktRcapCkptItmId;

    @Column(name = "mrkt_rcap_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("시장 리캡 ID")
    private Long mrktRcapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrkt_rcap_id", foreignKey = @jakarta.persistence.ForeignKey(name = "mrkt_rcap_ckpt_itm_fk01"))
    private MarketRecap marketRecap;

    @Column(name = "titl", length = 200)
    @Comment("제목")
    private String titl;

    @Column(name = "cntt", length = 500)
    @Comment("내용")
    private String cntt;

    @Builder
    public MarketRecapCheckpointItem(MarketRecap marketRecap, String titl, String cntt) {
        this.marketRecap = marketRecap;
        this.titl = titl;
        this.cntt = cntt;
    }
}
