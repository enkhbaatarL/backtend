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
 * 뉴스 이벤트 관계 Entity
 */
@Entity
@Table(
    name = "tmd_nws_evnt_rel",
    indexes = {
        @Index(name = "tmd_nws_evnt_rel_ix01", columnList = "nws_evnt_id"),
        @Index(name = "tmd_nws_evnt_rel_ix02", columnList = "nws_smry_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nws_evnt_rel_ux01", columnNames = {"nws_evnt_id", "nws_smry_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_뉴스 이벤트 관계")
public class NewsEventRelationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_evnt_rel_id", columnDefinition = "INT UNSIGNED")
    @Comment("뉴스 이벤트 관계 ID")
    private Long newsEventRelationshipId;

    @Column(name = "nws_evnt_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("뉴스 이벤트 ID")
    private Long newsEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_evnt_id", nullable = false)
    @Comment("뉴스 이벤트")
    private NewsEvent newsEvent;

    @Column(name = "nws_smry_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("이벤트에 속하는 뉴스의 뉴스 요약 ID")
    private Long newsSummaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", nullable = false)
    @Comment("뉴스 요약")
    private NewsSummary newsSummary;

    @Builder
    public NewsEventRelationship(NewsEvent newsEvent, NewsSummary newsSummary) {
        this.newsEvent = newsEvent;
        this.newsSummary = newsSummary;
    }
}
