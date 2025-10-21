package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tif.RefinitivNewsData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        @Index(name = "tmd_nws_sig_ix_01", columnList = "data_reg_sn, first_created"),
        @Index(name = "tmd_nws_sig_ix_02", columnList = "nws_mrkt_ctgr_cd")
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

    @Column(name = "data_reg_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("Refinitiv 뉴스 데이터등록일련번호")
    private Long dataRegistrationSn;

    @Column(name = "first_created", nullable = false)
    @Comment("Refinitiv 뉴스 최초생성일시")
    private LocalDateTime firstCreated;

    // 논리적 연관관계 (조회용, insertable=false, updatable=false)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_reg_sn", referencedColumnName = "data_reg_sn",
        insertable = false, updatable = false)
    @JoinColumn(name = "first_created", referencedColumnName = "first_created",
        insertable = false, updatable = false)
    @Comment("Refinitiv 뉴스 데이터")
    private RefinitivNewsData refinitivNewsData;

    @Column(name = "nws_mrkt_ctgr_cd", length = 20, nullable = false)
    @Comment("뉴스시장카테고리코드 - 시장 분류 (Market Category) 원유, 천연가스, 반도체, EV배터리 등으로 분류")
    private String nwsMrktCtgrCd;

    @Column(name = "sig", precision = 4, scale = 2)
    @Comment("중요도")
    private BigDecimal sig;

    @Lob
    @Column(name = "derv_rsn", columnDefinition = "TEXT")
    @Comment("도출이유")
    private String dervRsn;

    @Builder
    public NewsSignificance(Long dataRegistrationSn, LocalDateTime firstCreated, String nwsMrktCtgrCd,
                           BigDecimal sig, String dervRsn) {
        this.dataRegistrationSn = dataRegistrationSn;
        this.firstCreated = firstCreated;
        this.nwsMrktCtgrCd = nwsMrktCtgrCd;
        this.sig = sig;
        this.dervRsn = dervRsn;
    }
}
