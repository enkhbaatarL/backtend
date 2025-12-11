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
 * 중요 뉴스 Entity
 */
@Entity
@Table(
    name = "tmd_sgt_nws",
    indexes = {
        @Index(name = "tmd_sgt_nws_ix01", columnList = "nws_sig_id"),
        @Index(name = "tmd_sgt_nws_ix02", columnList = "crtr_dt")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_sgt_nws_ux01", columnNames = {"nws_sig_id"})
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
    @JoinColumn(name = "nws_sig_id", nullable = false)
    @Comment("뉴스 중요도")
    private NewsSignificance newsSignificance;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자")
    private String crtrDt;

    @Column(name = "scre", precision = 4, scale = 2, nullable = false)
    @Comment("점수")
    private BigDecimal score;

    @Builder
    public SignificantNews(NewsSignificance newsSignificance, String crtrDt,
                          BigDecimal score) {
        this.newsSignificance = newsSignificance;
        this.crtrDt = crtrDt;
        this.score = score;
    }
}
