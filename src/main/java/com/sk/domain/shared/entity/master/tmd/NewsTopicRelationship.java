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
 * 뉴스 토픽 관계 Entity
 */
@Entity
@Table(
    name = "tmd_nwstopreld",
    indexes = {
        @Index(name = "tmd_nwstopreld_ix01", columnList = "crtr_dt"),
        @Index(name = "tmd_nwstopreld_ix02", columnList = "nws_top_id"),
        @Index(name = "tmd_nwstopreld_ix03", columnList = "nws_smry_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nwstopreld_ux01", columnNames = {"crtr_dt", "nws_top_id", "nws_smry_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_뉴스 토픽 관계")
public class NewsTopicRelationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_top_rel_id", columnDefinition = "INT UNSIGNED")
    @Comment("뉴스 토픽 관계 ID")
    private Long newsTopicRelationshipId;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자 (YYYYMMDD)")
    private String crtrDt;

    // 조인용 외래키 컬럼 (값 조회용)
    @Column(name = "nws_top_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("뉴스 토픽 ID")
    private Long newsTopicId;

    /**
     * 뉴스 토픽 마스터 엔티티 (tmd_nwstopm 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_top_id", nullable = false)
    @Comment("뉴스 토픽")
    private NewsTopic newsTopic;

    @Column(name = "nws_smry_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("뉴스 요약 ID")
    private Long newsSummaryId;

    /**
     * 뉴스 요약 엔티티 (tmd_nws_smry 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", nullable = false)
    @Comment("뉴스 요약")
    private NewsSummary newsSummary;

    @Builder
    public NewsTopicRelationship(String crtrDt, NewsTopic newsTopic, NewsSummary newsSummary) {
        this.crtrDt = crtrDt;
        this.newsTopic = newsTopic;
        this.newsSummary = newsSummary;

        if (newsTopic != null) this.newsTopicId = newsTopic.getNewsTopicId();
        if (newsSummary != null) this.newsSummaryId = newsSummary.getNewsSummaryId();
    }
}
