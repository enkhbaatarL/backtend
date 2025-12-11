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
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 시장 리캡 하이라이트 이벤트 Entity
 */
@Entity
@Table(
    name = "tmd_mrkt_rcap_hl_evnt",
    indexes = {
        @Index(name = "tmd_mrkt_rcap_hl_evnt_ix01", columnList = "mrkt_rcap_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mrkt_rcap_hl_evnt_ux01", columnNames = {"mrkt_rcap_id", "nm_kr"})
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_마켓 리캡 투데이 하이라이트 이벤트")
public class MarketRecapHighlightEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_rcap_hl_evnt_id", columnDefinition = "INT UNSIGNED")
    @Comment("시장 리캡 하이라이트 이벤트 ID")
    private Long mrktRcapHlEvntId;

    @Column(name = "mrkt_rcap_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("시장 리캡 ID")
    private Long mrktRcapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrkt_rcap_id", nullable = false)
    @Comment("시장 리캡")
    private MarketRecap marketRecap;

    @Column(name = "nm_kr", length = 200, nullable = false)
    @Comment("이벤트 한글명")
    private String nameKr;

    @Column(name = "nm_en", length = 200, nullable = false)
    @Comment("이벤트 영문명")
    private String nameEn;

    @Column(name = "scre", precision = 5, scale = 2)
    @Comment("이벤트 점수")
    private BigDecimal score;

    @Builder
    public MarketRecapHighlightEvent(MarketRecap marketRecap, String nameKr, String nameEn, BigDecimal score) {
        this.marketRecap = marketRecap;
        this.nameKr = nameKr;
        this.nameEn = nameEn;
        this.score = score;
    }

    /**
     * 점수 업데이트
     */
    public void updateScore(BigDecimal score) {
        this.score = score;
    }

    /**
     * 이벤트명 업데이트
     */
    public void updateName(String nameKr, String nameEn) {
        this.nameKr = nameKr;
        this.nameEn = nameEn;
    }
}
